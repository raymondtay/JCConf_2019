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

import cats._
import cats.implicits._
import cats.effect._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._
import scala.util.control._

import java.util.concurrent._
import java.util.concurrent.atomic._

/**
 * This example gives you an example of building a cancellable object via the
 * [[IO.cancelable]] builder. The cancellation logic is held in the
 * [[IO]] effect that is also known as the cancellation token/logic.
 *
 * @author Raymond Tay
 * @version 1.0
 */
object Sleeper extends IOApp {

  def sleep(d: FiniteDuration)(implicit ec: ScheduledExecutorService): IO[Unit] =
    IO.cancelable { cb =>
      // Schedules task to run after delay
      val run = new Runnable { def run() = cb(Right(())) }
      val future = ec.schedule(run, d.length, d.unit)
  
      // Cancellation logic, suspended in IO
      IO(future.cancel(true))
    }

  def run(args: List[String]) : IO[ExitCode] = {

    /* A scheduled thread pool */
    implicit val sc = Executors.newScheduledThreadPool(1)

    /* Implicit context for thread pool */
    implicit val ScheduledPool = ExecutionContext.fromExecutor(sc)

    val idletime : FiniteDuration = 3.seconds
    (for {
      f         <- IO.shift *>
                   IO(println(s"[${Thread.currentThread.getName}] Going to sleep for $idletime ")) *>
                   sleep(idletime).start /* fiber started in thread pool */
      aftermath <- timer.sleep(idletime) *>
                   IO(println(s"[${Thread.currentThread.getName}] Awoke after $idletime")) *> f.cancel
      _         <- IO(sc.shutdown())
     } yield aftermath).unsafeRunSync

    IO(ExitCode.Success)
  }

}

