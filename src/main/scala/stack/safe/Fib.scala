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

package stack.safe

import cats._
import cats.implicits._
import cats.data._
import cats.effect._
import cats.free._
import scala.annotation._

/**
 * There many ways to write recursive functions like the fibonacci sequence and
 * the interesting thing here is that Scala allows you to take advantage of
 * tail-call optimization to solve some of these problems; and over here, there
 * are cases where leveraging cats-free and/or cats-effect idiomatically you can
 * also deliver good performance.
 *
 * @author Raymond Tay
 * @version 1.0
 */


// Safe.
trait ClassicFib {
  
  @tailrec
  final def fibTCO(n : Int, a : Long = 0, b : Long = 1) : Long =
    if (n <= 0) a else fibTCO(n - 1, b, a + b)

  @tailrec
  final def fibTCO2(n : Int, a : Long = 0, b : Long = 1) : Id[Long] =
    if (n <= 0) Monad[Id].pure(a) else fibTCO2(n - 1, b , a + b)

}

trait IOFibUnsafe {

  // Classic mistake when you `cats` and assumes it trampolines
  // which doesn't happen. `flatMap` is treated as "special" in the IO Monad
  def fibNoTCO(n : Int, a : Long = 0, b : Long = 1) : Id[Long] =
    Monad[Id].pure(a + b).flatMap {
      b2 =>
        if (n > 0) 
          fibNoTCO(n - 1, b, b2)
        else a
    }

}

trait IOFibSafe {

  // A typical approach via IO monad.
  def fib(n: Int, a: Long = 0, b: Long = 1): IO[Long] =
    IO(a + b).flatMap { b2 =>
        if (n > 0) 
          fib(n - 1, b, b2)
        else 
          IO.pure(a)
      }

  // Using `cats.free`, you can do it too! and there's no reliance on the IO
  // monad for the fact there is no real need. Experiment things and try it
  // out!
  def trampolineFib(n: Int, a: Long = 0, b : Long = 1): Trampoline[Long] =
    if (n > 0) Trampoline.defer(trampolineFib(n - 1, b , a + b))
    else Trampoline.done(a)

}


object Fib extends IOApp with ClassicFib with IOFibSafe with IOFibUnsafe {

  def run(args: List[String]) : IO[ExitCode] = {
    val n = 100000

    println(fibTCO(n))
    println(fibTCO2(n))
    println(fib(n).unsafeRunSync)
    println(trampolineFib(n).run)

    //Either.catchNonFatal(fibNoTCO(n)) /* Bomb! */

    IO(ExitCode.Success)
  }

}

