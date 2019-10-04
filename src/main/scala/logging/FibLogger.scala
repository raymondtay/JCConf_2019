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

package logging

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import scala.concurrent.duration._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger


/**
 * Leveraging the `log4cats` framework by Chris Davenport, you can easily add logging to
 * your algorithm if you so wished !
 */
object FibLogger extends IOApp {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  def fib[F[_]:Sync:ContextShift](n: Int, a: Long = 0, b: Long = 1): F[Long] =
    Sync[F].suspend {
      if (n == 0) Logger[F].info(s"=> Done $a") *> Sync[F].pure(a) else {

        val next = Logger[F].info(s"=> Next $a") *> fib(n - 1, b, a + b)
        // Every 100 cycles, introduce a logical thread fork
        if (n % 100 == 0)
          Logger[F].info(s"Context Shift!") *> ContextShift[F].shift *> next
        else
          next
      }
    }

  def doSomething[F[_]:Sync:ContextShift](n : Int, a: Long = 0, b: Long = 1): F[Long] =
    Logger[F].info("Logging Started.") *>
    fib(n, a, b) >>= { result => Logger[F].info("Logging Ended.") *> Sync[F].pure(result) }


  def run(args: List[String]) : IO[ExitCode] = {

    println(doSomething[IO](100).unsafeRunSync)

    IO(ExitCode.Success)
  }

}
