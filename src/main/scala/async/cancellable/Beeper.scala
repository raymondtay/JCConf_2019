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
 * [[IO.cancelable]] builder. The cancellation logic is held in the [[IO]]
 * effect that is also known as the cancellation token/logic.
 *
 * @author Raymond Tay
 * @version 1.0
 */
object Beeper extends IOApp {

  def beep(implicit SC : ScheduledExecutorService) =
    IO.cancelable[Unit] { cb =>
      lazy val beeper : Runnable = new Runnable {
        def run() = {
          println(s"[${Thread.currentThread.getName}] >> beep! <<")
        }
      }
      val beeperHandle = SC.scheduleAtFixedRate(beeper, 1, 1, SECONDS)
      
      IO {
        println(s"[${Thread.currentThread.getName}] >> Beeping canceled! <<")
        beeperHandle.cancel(false)
      }
    }

  def run(args: List[String]) : IO[ExitCode] = {

    /* A scheduled thread pool of size 1 */
    implicit val sc = Executors.newScheduledThreadPool(1)

    /* Implicit context for thread pool */
    implicit val ScheduledPool = ExecutionContext.fromExecutor(sc)

    (for {
      f         <- beep.start
      aftermath <- timer.sleep(3 seconds) *> f.cancel *> IO(sc.shutdown)
     } yield aftermath).unsafeRunSync

    IO(ExitCode.Success)
  }

}

