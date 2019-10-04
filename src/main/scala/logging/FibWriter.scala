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

trait FibonacciWithLogging {

  /**
    * Fibonacci evaluation via IO Monad
    */
  def fib(n: Int, a: Long = 0, b: Long = 1)(implicit cs: ContextShift[IO]): IO[Long] =
    IO.suspend {
      if (n == 0) IO.pure(a) else {

        val next = fib(n - 1, b, a + b)
        // Every 100 cycles, introduce a logical thread fork
        if (n % 100 == 0)
          cs.shift *> next
        else
          next
      }
    }

  /**
    * Fibonacci evaluation (via IO Monad) with a Writer Monad; as with the
    * regular Writer Monad implementation, you can only extract the information
    * in its entirety at the end of the evaluation.
    *
    * Check out the example to see!
    */
  def fibW(n: Int, a: Long = 0, b: Long = 1)
          (implicit cs: ContextShift[IO], W: IO[Writer[List[String],Long]]): IO[Writer[List[String],Long]] =
    IO.suspend {
      if (n == 0) W >>= { writer => IO{ Writer(writer.written :+ s"=> Done $a", a) } } else {

        def next(WW: IO[Writer[List[String], Long]]) =
          WW >>= {writer => fibW(n - 1, b, a + b)(cs, IO{writer.tell(List(s"=> Next $a"))})}

        // Every 100 cycles, introduce a logical thread fork
        if (n % 10 == 0)
          W >>= { writer => cs.shift *> next(IO{writer.tell(List(s"=> Context Shift !"))}) }
        else
          W >>= { writer => next(IO{writer.tell(List(s"=> regular "))}) }
      }
    }

}

object FibWriter extends IOApp with FibonacciWithLogging {

  def run(args: List[String]) : IO[ExitCode] = {

    implicit val writer : IO[Writer[List[String],Long]] =
      IO{Writer.value(0)}

    println(fibW(100).unsafeRunSync.value)
    println(fibW(100).unsafeRunSync.written)

    IO(ExitCode.Success)
  }
}
