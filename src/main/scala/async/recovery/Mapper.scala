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

package async.recovery

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._

/**
 * This example is a polymorphic approach to encoding a solution where we
 * increment each element of a list by 1, iff each element is a positive
 * number; if any element in the list is a negative number an error would be
 * raised.
 *
 * An effect, represented by IO, when runs into errors would abort the entire
 * computation (i.e. fail-fast) and cancellation does not apply.
 *
 * @author Raymond Tay
 * @version 1.0
 */
object Mapper extends IOApp {
  import ExitCase._

  // Obtain a value and increment the value by 1 if positive
  // otherwise an exception is given to indicate error condition; noticed that
  // no exception is physically _thrown_.
  // In the examples below, you will see one way of recovering from errors.
  def convertToAsync[A:Numeric,F[_]:Async](value : A, compareBy: A, op: A => A) =
    Async[F].async[A]{ cb =>
      val G = Functor[Id]
      if (implicitly[Numeric[A]].gt(value, compareBy)) 
        cb(Right(G.fmap[A,A](value)(op(_))))
      else
        cb(Left(new Exception(s"No negative values: $value")))
    }

  def run(args: List[String]) : IO[ExitCode] = {
   
    val increOp: Int => Int = _ + 1

    lazy val result = 
      List(1, 2, 3)
        .map(convertToAsync[Int,IO](_, 0, increOp))
        .foldLeft(IO(0))((acc,e) => e.map2(acc)(_ + _))

    lazy val result2 = 
      List(1, 2, -3)
        .map(n => convertToAsync[Int, IO](n, 0, increOp)
        .handleErrorWith(err => IO(increOp(math.abs(n)))))
        .foldLeft(IO(0))((acc, e) => e.map2(acc)(_ + _))

    // cats.Parallel
    lazy val result3 = 
      (convertToAsync[Int,IO](1, 0, increOp).handleErrorWith(err => IO(increOp(math.abs(1)))),
        convertToAsync[Int,IO](-2, 0, increOp).handleErrorWith(err => IO(increOp(math.abs(-2)))),
        convertToAsync[Int,IO](3, 0, increOp).handleErrorWith(err => IO(increOp(math.abs(3))))).parMapN{ (a, b, c) => a + b + c }

    lazy val result4 = 
      List(1,-2,3)
        .map(x =>convertToAsync[Int,IO](x, 0, increOp).handleErrorWith(err => IO(increOp(math.abs(x))))).parSequence.unsafeRunSync

    // All of the successful examples should return '9' since
    // List(1,2,3).map(_+1).reduce(_+_) == 9
    println(s"=> Regular ${result.unsafeRunSync}") 
    println(s"=> Error recovery ${result2.unsafeRunSync}") 
    println(s"=> parMapN ${result3.unsafeRunSync}") 
    println(s"=> parSequence ${result4.reduce(_ + _)}") 

    IO(ExitCode.Success)
  }
}
