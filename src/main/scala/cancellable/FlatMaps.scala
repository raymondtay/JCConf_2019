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

/**
  * In regular Scala, flatMap(s) are equivalent to the for-loop and you can
  * either form interchangeably. In the cats-effect framework, there's a
  * mention on flatMaps cannot be canceled automatically. I am a bit puzzled by
  * this statement so i set to figure out what is meant by this exactly.
  *
  * What is the key insight behind this and what does it mean for the developer
  * when using this library?
  */

case class Resource() {
  private val isActive = new AtomicBoolean(false)
  def isDone : Boolean = isActive.get == true
  def setActive = {println("[+] Active"); isActive.set(true)}
  def setNonactive = {println("[-] Non Active"); isActive.set(false)}
}

trait Examples {
  import Random._
  implicit val ec = ExecutionContext.global

  def setNonactive(sharedResource: Resource)(implicit ec:ExecutionContext) = 
    IO.cancelable[Resource] {
      cb =>
        ec.execute{() =>
          if(sharedResource.isDone)
            try {
              sharedResource.setNonactive
              cb(Right(sharedResource))
            } catch { case NonFatal(e) => cb(Left(new Exception("oh no!"))) }
          else 
            cb(Right(sharedResource))
        }
        IO{sharedResource.setActive; println("<< Set to active >>")}
    }

  def setActive(sharedResource: Resource)(implicit ec:ExecutionContext) = 
    IO.cancelable[Resource] {
      cb =>
        ec.execute{() =>
          if(!sharedResource.isDone)
            try {
              sharedResource.setActive
              cb(Right(sharedResource))
            } catch { case NonFatal(e) => cb(Left(new Exception("oh no!"))) }
          else
            cb(Right(sharedResource))
        }
        IO{sharedResource.setNonactive; println("<< Set to non-active >>")}
    }
}


object FlatMaps extends IOApp with Examples {

  def run(args : List[String]) : IO[ExitCode] = {
    // A shared resource
    val resource = Resource()

    // This is cancelable.
    def task =
      setActive(resource).start.flatMap { f =>
        setNonactive(resource).start.flatMap { g =>
          f.cancel.map(_ => {println(">> F is canceled")}) *>
          g.cancel.map(_ => {println(">> G is canceled")}) *>
          IO((f.join, g.join))
        }
      }

    val (l, r) = task.unsafeRunSync
    val (ll, rr) = task.unsafeRunSync
    println(s"[-] Are we done using resource? ${resource.isDone}")
    IO.race(IO(l, r), IO.race(ll, rr)).unsafeRunSync
    println(s"[+] Are we done using resource? ${resource.isDone}")


    IO(ExitCode.Success)
  }

}
