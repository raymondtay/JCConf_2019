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

package cancellable

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import scala.concurrent._
import scala.concurrent.duration._


object Fib extends IOApp {

  def fib(n: Int, a: Long = 0, b: Long = 1): IO[Long] =
    IO.suspend {
       if (n <= 0) IO.pure(a) else {
         val next = fib(n - 1, b, a + b)
     
         // Every 100-th cycle, check cancellation status
         if (n % 3 == 0)
           IO.cancelBoundary *> next
         else
           next
       }

       //IO(println("canceled")) *> IO(0L) 
    }

  def run(args: List[String]) : IO[ExitCode] = {

    val fibTask =
      for {
        f <- fib(100).start
        //_ <- f.cancel /* Just because it compiles does not mean it will behave properly; know your types! */
        r <- f.join
      } yield r

    // Option 1.
    println(fibTask.unsafeRunSync)

    // Option 2.
    // println(fib(100).unsafeRunSync)

    IO(ExitCode.Success)
  }

}
