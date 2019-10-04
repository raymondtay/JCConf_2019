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

package uncancellable

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import scala.concurrent._
import scala.concurrent.duration._

/**
  * Lifted directly from this issue [#267](https://github.com/typelevel/cats-effect/issues/267)
  */
object Uncancellable1 extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val Timeout = 2.milli 
    val promise = Promise[Unit] // will never finish
    val loooongIO = IO.fromFuture(IO(promise.future))

    for {
      _ <- IO(println("test uncancellable"))
      _ <- loooongIO.guaranteeCase {
        case ExitCase.Completed  => IO(println("Completed"))
        case ExitCase.Canceled   => IO(println("Canceled: Before")) *> IO.sleep(Timeout) *> IO(println("Canceled: After"))
        case ExitCase.Error(err) => IO(println(s"Error($err)"))
      }
    } yield {
      ExitCode.Success
    }
  }
}

/**
  * The example here is slightly more complicated than [[Uncancellable1]]
  * in the sense that the never ending computation is held within the "bracket"
  * and it not longer blocks all other work from progressing but also leaks 
  */
object Uncancellable2 extends IOApp {

  def doSomethingSerious(f: Fiber[IO,Int], sem1: Semaphore[IO], sem2: Semaphore[IO]) = {

    val Timeout = 2.milli 
    val promise = Promise[Int] // will never finish
    val loooongIO : IO[Int] = IO.fromFuture(IO(promise.future))

    (
      IO(println("Acquiring")) *> // kinda like logging but its not; don't do this.
      sem1.acquire *> IO.raiseError(new Exception("Ops")) *>
      sem2.acquire *>
      IO(println("Acquired"))
    ).handleErrorWith{err => IO(println("Error caught, cancelling Fiber...")) *> f.cancel}
     .bracket{_ =>
       IO(print("Start..")) *> IO(0) >>=
       ((x:Int) => IO(x+1)) >>=
       ((x:Int) => IO(println(s"intermediate evaluation: $x")) *>
         loooongIO.guaranteeCase{
           case ExitCase.Completed  => IO(println("Completed"))
           case ExitCase.Canceled   => IO(println("Canceled: Before")) *> IO.sleep(Timeout) *> IO(println("Canceled: After"))
           case ExitCase.Error(err) => IO(println(s"Error($err)"))
         }
       )
     }{  _ => sem2.release *> sem1.release *> IO(println("Released!"))} // this is never called, what happens to this?
  }

  def run(args: List[String]) : IO[ExitCode] = {

    // Ok.
    val ioa = for {
      a <- Semaphore[IO](1)
      b <- Semaphore[IO](1)
      f <- IO.cancelable[Int]{ cb => Right(44); IO(println("Canceled")) }.start // fiber does not block, so its ok ;)
      _ <- List(doSomethingSerious(f, b, a),
                doSomethingSerious(f, b, a),
                doSomethingSerious(f, b, a)).parSequence
    } yield ExitCode.Success

    ioa.unsafeRunSync  // hung 

    IO(ExitCode.Success)
  }

}

