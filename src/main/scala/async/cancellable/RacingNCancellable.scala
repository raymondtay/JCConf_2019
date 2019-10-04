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

package async.cancellable


import cats.effect._
import cats._, cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._
import scala.language.implicitConversions
import java.util.concurrent.atomic._

// Pseudo service that returns a [[scala.concurrent.Future]] of String
trait Service { def getResult(): Future[String] }

/**
 * This example demonstrates that [[Concurrent]] datatypes can be cancelable
 * but [[Async]] is not.
 *
 * @author Raymond Tay
 * @version 1.0
 */
object RacingNCancellable extends IOApp {

  implicit def buildRacers[A, F[_]:Concurrent](ioas: Seq[F[A]]) = {
    def go(acc: F[A], ios: F[A]*) : F[_] = 
      ios match {
        case Nil => acc
        case h :: t => Concurrent[F].race(h, go(acc, t:_*) )
      }

    ioas match {
      case Nil => Concurrent[F].delay{()}
      case h :: t => go(h, t:_*)
    }
  }

  // convenient function create a psuedo service
  def service: Service = new Service {
    def getResult() = Future{"Hello!"}
  }
 
  /**
   * [[Asynchronous]] and [[Concurrent]] process that is cancelable.
   * When you run this example, you would notice the cancellation occurs.
   *
   * @param idx async task's id
   * @return [[Concurrent]] object
   */
  def processServiceResultConc[F[_]: Concurrent](idx: Int) : F[String] =
    Concurrent[F].asyncF{ (cb: Either[Throwable, String] => Unit) => 
      val active = new AtomicBoolean(true)
      if (active.getAndSet(false))
        service.getResult().onComplete {
          case Success(s) => println(s"=> Concurrent-[${Thread.currentThread.getName}] Yes:$idx"); cb(Right(s+"_"+idx))
          case Failure(e) => println(s"=> Concurrent-[${Thread.currentThread.getName}] No:$idx "); cb(Left(e))
        }

      Concurrent[F].delay{
        if (active.getAndSet(false))
          println(s"Task-${idx} is canceled and set to false!")
        else 
          println(s"Task-${idx} is no longer active canceled")
      }
    }

  /**
   * Asynchronous process that is non-cancelable. An obvious difference between
   * this and the [[Concurrent]] process approach is that this approach is not
   * cancelable.
   *
   * @param idx async task's id
   * @return [[Concurrent]] object
   */
  def processServiceResultAsync[F[_]: Async](idx: Int) : F[String] =
    Async[F].async{ (cb: Either[Throwable, String] => Unit) => 
      service.getResult().onComplete {
        case Success(s) => println(s"=> Async-[${Thread.currentThread.getName}] Yes:$idx"); cb(Right(s+"_"+idx))
        case Failure(e) => println(s"=> Async-[${Thread.currentThread.getName}] No:$idx "); cb(Left(e))
      }
    }

  def runCancelable =
    IO{println("\n===== Start =======>")} *>
    IO(println(Seq(
      processServiceResultConc[IO](1),
      processServiceResultConc[IO](2), /* odd that this is almost always picked ! */
      processServiceResultConc[IO](3),
      processServiceResultConc[IO](4),
      processServiceResultConc[IO](5),
      processServiceResultConc[IO](6),
      processServiceResultConc[IO](7),
      processServiceResultConc[IO](8),
      processServiceResultConc[IO](9),
      processServiceResultConc[IO](10)
    ).unsafeRunSync)) *>
    IO(println("\n===== End =======>"))

  def runNonCancelable =
    IO(println("\n===== Start =======>")) *>
    IO(println(Seq(
      processServiceResultAsync[IO](1),
      processServiceResultAsync[IO](2), /* odd that this is almost always picked ! */
      processServiceResultAsync[IO](3),
      processServiceResultAsync[IO](4),
      processServiceResultAsync[IO](5),
      processServiceResultAsync[IO](6),
      processServiceResultAsync[IO](7),
      processServiceResultAsync[IO](8),
      processServiceResultAsync[IO](9),
      processServiceResultAsync[IO](10)
    ).unsafeRunSync)) *>
    IO(println("\n===== End =======>"))

  def run(args: List[String]) : IO[ExitCode] = {
    // option 1. Concurrent processes that are cancelable
    runCancelable.unsafeRunSync
    // option 2. Asynchronous processes that are NOT cancelable
    runNonCancelable.unsafeRunSync

    IO(ExitCode.Success)
  }

}

