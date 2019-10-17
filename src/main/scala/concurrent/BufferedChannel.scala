/*
 * Copyright 2019 Raymond Tay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package concurrent

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import scala.concurrent.duration._

/**
  * This example is translated from Simon Marlow's book on
  * [Parallel and Concurrent Programming in Haskell](https://simonmar.github.io/pages/pcph.html)
  * where the original code developed by Simon is highlighted on page 135 of the 
  * book in the paragraph entitled =>
  * "MVar as a building block: Unbounded Channels"
  *
  * If you are interested in writing code in this style, well hopefully its
  * sufficient proof that you can do it this way too !
  *
  * @author Raymond Tay
  * @version 1.0
  *
  */

object BufferedChannel extends cats.effect.IOApp {
  def newChannel[A,F[_]:Concurrent] : F[Channel[A,F]] =
    for {
      r <- MVar.uncancelableEmpty[F, Item[A,F]]
      a <- MVar.uncancelableOf[F, Stream[A,F]](r)
      b <- MVar.uncancelableOf[F, Stream[A,F]](r)
    } yield Channel(a, b)

  // Same effect as that of the `uncancelableXXX` equivalent; but it can
  // introduce memory leaks if i/you are not careful.
  def newChannel2[A, F[_]:Concurrent] : F[Channel[A,F]] = for {
    r <- MVar.empty[F, Item[A,F]]
    a <- MVar.of[F, Stream[A,F]](r)
    b <- MVar.of[F, Stream[A,F]](r)
  } yield Channel(a, b)

  def readChannel[A, F[_]:Concurrent](ch: Channel[A,F]) : F[A] = {
    for {
      stream <- ch.reader.take
      item   <- stream.read
      _      <- ch.reader.put(item.tail)
    } yield item.head
  }

  def writeChannel[A, F[_]:Concurrent](ch: Channel[A,F], value : A) : F[Unit] = {
    for {
      newHole <- MVar.empty[F, Item[A,F]]
      oldHole <- ch.writer.take
      _ <- ch.writer.put(newHole)
      _ <- oldHole.put(Item(value, newHole))
    } yield ()
  }

  def dupChan[A, F[_]:Concurrent](ch: Channel[A,F]) : F[Channel[A,F]] = {
    for {
      hole       <- ch.writer.read
      newReadVar <- MVar.uncancelableOf[F, Stream[A,F]](hole)
    } yield Channel(newReadVar, ch.writer)
  }

  def unGetChan[A, F[_]:Concurrent](ch: Channel[A,F], value: A) : F[Unit] = {
    for {
      newReadEnd <- MVar.empty[F, Item[A,F]]
      readEnd    <- ch.reader.take
      _ <-  newReadEnd.put(Item(value, readEnd))
      _ <-  ch.reader.put(newReadEnd)
    } yield {}
  }

  def sumChannel[F[_]:Concurrent](ch: Channel[Int,F], sum: Long) : F[Long] = {
    val F = implicitly[Concurrent[F]]
    for {
      flag   <- ch.reader.read
      empty  <- flag.isEmpty
      result <- if (empty) F.pure(sum) else 
        F.suspend {
          for {
            stream <- ch.reader.take
            item   <- stream.read
            _      <- ch.reader.put(item.tail)
            sum    <- sumChannel(ch, sum+item.head.toLong)
          } yield sum
        }
    } yield result
  }

  def run(args: List[String]): IO[ExitCode] = {
   
    // Ungetting the channel sounds a little weird and unprofessional,
    // but what it really does is to push a value back onto the read end of the channel.
    def ungetChanTask[F[_]:Concurrent] =
      for {
        channel <- newChannel[Int,F]
        _ <- writeChannel(channel, 1)
        a <- readChannel(channel)
        _ <- unGetChan(channel, 2)
        b <- readChannel(channel)
      } yield (a, b)

    // duplicate a channel after two values are deposited
    // and then write two more values (i.e. 2) into the duplicate
    // channel and draw some values from it.
    def dupChanTask[F[_]:Concurrent] = 
      for {
        channel <- newChannel[Int,F]
        _   <- writeChannel(channel, 1)
        _   <- writeChannel(channel, 2)
        d   <- readChannel(channel)
        dup <- dupChan(channel)
        _   <- writeChannel(dup, 3)
        _   <- writeChannel(dup, 4)
        _   <- writeChannel(dup, 5)
        a   <- readChannel(dup)
        b   <- readChannel(dup)
        c   <- readChannel(dup)
      } yield (a, b, c, d)

    // Putting data values in and draining them out
    def putNDrain =
      for {
        channel <- newChannel[Int, IO]
        a  <- writeChannel(channel, 1).start
        b  <- writeChannel(channel, 2).start
        c  <- writeChannel(channel, 3).start
        d  <- writeChannel(channel, 4).start
        el1 <- readChannel(channel)
        el2 <- readChannel(channel)
        el3 <- readChannel(channel)
        el4 <- readChannel(channel)
        _ <- a.join // in this scenario, it has no effect as there're no downstream operations
        _ <- b.join
      } yield (el1, el2, el3, el4)

    // Put data values and collects them applying a reduction.
    def sumTask =
      for {
        channel <- newChannel[Int,IO]
        _ <- writeChannel(channel, 1).start
        _ <- writeChannel(channel, 2).start
        _ <- writeChannel(channel, 3).start
        _ <- writeChannel(channel, 4).start
        _ <- writeChannel(channel, 5).start
        _ <- writeChannel(channel, 6).start
        _ <- writeChannel(channel, 7).start
        _ <- writeChannel(channel, 8).start
        _ <- writeChannel(channel, 9).start
        sum <- sumChannel(channel, 0L)
      } yield sum

    // Concurrent writes with interfering reads.
    def sumTask2 =
      for {
        channel <- newChannel[Int,IO]
        _ <- writeChannel(channel, 1).start
        _ <- writeChannel(channel, 2).start
        _ <- writeChannel(channel, 3).start
        _ <- writeChannel(channel, 4).start
        _ <- readChannel(channel) *> readChannel(channel) *> readChannel(channel)
        _ <- writeChannel(channel, 5).start
        _ <- writeChannel(channel, 6).start
        _ <- readChannel(channel) *> readChannel(channel) *> readChannel(channel)
        _ <- writeChannel(channel, 7).start
        _ <- writeChannel(channel, 8).start
        _ <- writeChannel(channel, 9).start
        sum <- sumChannel(channel, 0L)
      } yield sum

    println(s"""
      Result of draining an channel is: ${putNDrain.unsafeRunSync}
      """)

    println(s"""
      Result of summing an channel with no reads is: ${sumTask.unsafeRunSync}
      """)

    println(s"""
      Result of summing an channel with interfering reads is: ${sumTask2.unsafeRunSync}
      """)

    println(s"""
      Result of duplicating an channel is: ${dupChanTask[IO].unsafeRunSync}
      """)

    //println((sumTask >> putNDrain >> dupChanTask[IO] >> ungetChanTask[IO]).unsafeRunSync)

    IO.pure(ExitCode.Success)
  }

}

