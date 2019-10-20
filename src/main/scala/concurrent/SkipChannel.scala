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

import cats._, data._, implicits._
import cats.effect._
import cats.effect.concurrent._
import scala.concurrent.duration._

/**
  * This code example is inspired from "Concurrent Haskell" paper by Simon
  * Peyton Jones, Andrew Gordon & Sigbjorn Finne
  *
  * A skip channel is useful when an intermittent source of high-bandwidth
  * information (e..g mouse movement events) is to be coupled to a process that
  * may only be able to deal with events at a lower rate (scrolling a window, for example).
  *
  * A read operation on a skip channel either returns the most-recently-written value (skipping
  * any values written previously) or else blocks if no write has been performed since the
  * last read.
  *
  * data SkipChan a = SkipChan (MVar (a, [MVar ()])) (MVar ())
  * newSkipChan :: IO (SkipChan a)
  * newSkipChan = do
  *     sem <- newEmptyMVar
  *     main <- newMVar (undefined, [sem])
  *     return (SkipChan main sem)
  *
  * putSkipChan :: SkipChan a -> a -> IO ()
  * putSkipChan (SkipChan main _) v = do
  *     (_, sems) <- takeMVar main
  *     putMVar main (v, [])
  *     mapM_ (\sem -> putMVar sem ()) sems
  *
  * getSkipChan :: SkipChan a -> IO a
  * getSkipChan (SkipChan main sem) = do
  *     takeMVar sem
  *     (v, sems) <- takeMVar main
  *     putMVar main (v, sem:sems)
  *     return v
  *
  * dupSkipChan :: SkipChan a -> IO (SkipChan a)
  * dupSkipChan (main,_) =
  *   newEmptyMVar >>= \sem ->
  *   takeMVar main >>= \(v,sems) ->
  *   putMVar main (v, sem:sems) >> return (SkipChan main sem)
  *
  * @author Raymond Tay
  * @version 1.0
  *
  */

trait SkipChannel {

  def newSkipChan[A:Monoid, F[_]:Concurrent] : F[SkipChan[A, F]] = {
    val M = implicitly[Monoid[A]]
    for {
      sem  <- MVar.uncancelableEmpty[F, Unit]
      main <- MVar.uncancelableOf[F, (A,List[MVar[F,Unit]])]((M.empty, List(sem)))
    } yield SkipChan(main, sem)
  }

  def putSkipChan[A, F[_]:Concurrent](sCh: SkipChan[A, F], value:A ) : F[Unit] = {
    val F = implicitly[Concurrent[F]]
    for {
      pair <- sCh.main.take
      _    <- sCh.main.put((value, List()))
      _    <- F.delay(pair._2.map(sem => sem.put(())))
     } yield { }
  }

  def getSkipChan[A, F[_]:Concurrent](sCh: SkipChan[A, F]) : F[A] = {
    for {
      pair <- sCh.main.take
      _    <- sCh.main.put( (pair._1, sCh.sem :: pair._2) )
    } yield pair._1
  }

 def dupSkipChan[A, F[_]:Concurrent](sCh: SkipChan[A, F]) : F[SkipChan[A, F]] = {
    for {
      sem  <- MVar.empty[F, Unit]
      pair <- sCh.main.take
      _    <- sCh.main.put( (pair._1, sem::pair._2) )
    } yield SkipChan(sCh.main, sem)
  }

}

object SkipChannnel extends cats.effect.IOApp with SkipChannel {

  def run(args: List[String]) : IO[ExitCode] = {
    val dupSkipChanTask = for {
      channel <- newSkipChan[Int, IO]
      _ <- putSkipChan(channel, 0)
      _ <- putSkipChan(channel, 2)
      _ <- putSkipChan(channel, 4)
      dup <- dupSkipChan(channel)
      _ <- putSkipChan(dup, 20)
      a <- getSkipChan(channel)    // this should return "20"
      _ <- putSkipChan(dup, 7)
      b <- getSkipChan(dup)    // this should return "7"
      c <- getSkipChan(channel)    // this should return "7"
    } yield (a, b, c)

    val putNDrainTask = for {
      channel <- newSkipChan[Int, IO]
      _ <- putSkipChan(channel, 0)
      _ <- putSkipChan(channel, 1)
      a <- getSkipChan(channel)     // this should return "1"
      _ <- putSkipChan(channel, 2)
      _ <- putSkipChan(channel, 3)
      b <- getSkipChan(channel)     // this should return "3"
      _ <- putSkipChan(channel, 4)
      _ <- IO.sleep(10.millis)
      c <- getSkipChan(channel)     // this should return "4"
    } yield (a, b, c)

    def putNDrainTaskAsync = for {
      channel <- newSkipChan[Int, IO]
      f <- getSkipChan(channel).start // this should return "0"
      r <- f.join
      _ <- putSkipChan(channel, 0)
      _ <- putSkipChan(channel, 1)
      _ <- putSkipChan(channel, 2)
    } yield r
 
    println(s"""
      Result of writing and reading a skip-channel (sync) is: ${putNDrainTask.unsafeRunSync}
      """)

    println(s"""
      Result of writing and reading a skip-channel (async) is: ${putNDrainTaskAsync.unsafeRunSync}
      """)

    println(s"""
      Result of writing, duplicating and reading a skip-channel is: ${dupSkipChanTask.unsafeRunSync}
      """)

    IO.pure(ExitCode.Success)
  }
}
