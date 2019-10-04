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

// Precious resource
case class PreciousResource() {
  var isDone = false
}

object Snatcher extends IOApp {

  def snatch(id: Int, s : MVar[IO,PreciousResource])(implicit timer : Timer[IO]) = {
    IO.cancelable[Int]{ cb =>
      s.take.flatMap { resource =>
        if (resource.isDone) {
          println(s"[task-$id] Nothing to do")
          IO(cb(Left(new Exception("Someone beat me to it !"))))
        } else {
          resource.isDone = true
          println(s"[task-$id] Snatched it !")
          IO(cb(Right(25)))
        }
      }.unsafeRunAsyncAndForget

      IO(println(s"[task-$id] Canceled")) *> IO.unit
    }
  }

  def run(args: List[String]) : IO[ExitCode] = {
    // Option 1.
    val task =
      for {
        m <- MVar.of[IO,PreciousResource](PreciousResource())
        a <- snatch(0, m).start
        b <- snatch(1, m).start
        r <- a.cancel
        s <- b.cancel
      } yield {
        (r,s)
      }
    task.unsafeRunSync

    // Option 2.
    println(task.unsafeRunAsync{
      case Right(v) => println("R>> " + v)
      case Left(v)  => println("L>> "  + v)
    })

    val raceToTheFinish =
      MVar.of[IO,PreciousResource](PreciousResource()).flatMap{ resource =>
        IO.race(snatch(0, resource), snatch(1, resource))
      }

    // Option 3.
    println(raceToTheFinish.unsafeRunSync)

    IO(ExitCode.Success)
  }
}
