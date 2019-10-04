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
import cats.data._
import cats.implicits._
import cats.effect._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.control._
import scala.util.Random

import java.util.concurrent.atomic._

trait MutuallyRecursive {
  import Random._
  implicit val ec = ExecutionContext.global

  def setNonactive(sharedResource: Resource)(implicit ec:ExecutionContext) = 
    IO.cancelable[Resource] {
      cb =>
        ec.execute(() =>
          if(sharedResource.isDone)
            try {
              sharedResource.setNonactive
              cb(Right(sharedResource))
            } catch { case NonFatal(e) => cb(Left(new Exception("oh no!"))) }
          else 
            cb(Right(sharedResource))
        )
        IO{sharedResource.setActive; println("<< Set to active >>")}
    }

  def setActive(sharedResource: Resource)(implicit ec:ExecutionContext) = 
    IO.cancelable[Resource] {
      cb =>
        ec.execute(() =>
          if(!sharedResource.isDone)
            try {
              sharedResource.setActive
              cb(Right(sharedResource))
            } catch { case NonFatal(e) => cb(Left(new Exception("oh no!"))) }
          else
            cb(Right(sharedResource))
        )
        IO{sharedResource.setNonactive; println("<< Set to non-active >>")}
    }
}

object MutualRecursion extends IOApp with MutuallyRecursive {

  def run(args : List[String]) : IO[ExitCode] = {
    // A shared resource
    val resource = Resource()

    // A atypical/typical way to describe a recursive computation
    // that's described in terms of one another.
    lazy val repeatActive : IO[Resource] =
      setActive(resource).flatMap { r =>
        repeatNonActive
      } 
    lazy val repeatNonActive : IO[Resource] =
      setNonactive(resource).flatMap { r =>
        repeatActive
      } 
 
    val cancelableR =
      for {
        f <- repeatActive.start
        _ <- timer.sleep(2 seconds) /* allow 'f' to go on for a while */
        _ <- f.cancel
      } yield ()

    cancelableR.unsafeRunSync
    println(s"Are we done using resource? ${resource.isDone}")

    IO(ExitCode.Success)
  }

}
