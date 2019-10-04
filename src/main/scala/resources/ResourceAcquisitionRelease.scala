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

package resources

import cats._, effect._, cats.implicits._

trait Example0 {

  /**
   * Canonical approach to safely obtain, process, releasing the 
   * ANY resource you might require.
   */
  def obtainRelease = 
    IO(println("=> 1. Grabbing resources")).bracket {
      _ => IO(println("=> 2. Processing the resource"))
    } { _ => 
      IO(println("=> 3. Release the resource"))
    }.handleErrorWith(error => IO(println("=> 4. Dealt with error")))

  // Caveat here is that the exception is suppressed and the developer can
  // decide how they wish to handle the errors.
  def obtainReleaseHandleErrors = 
    IO(println("=> 1. Grabbing resources")).bracket {
      _ =>
        IO(println("=> 2. Processing the resource")) *> IO.raiseError(new RuntimeException("Ops!"))
    } { _ => 
      IO(println("=> 3. Release the resource"))
    }.handleErrorWith(error => IO(println("=> 4. Dealt with error")))

}

trait Examples extends Example0

object ResourceAcquisitionRelease extends IOApp with Examples {

  def run(args: List[String]) : IO[ExitCode] = {

    (obtainRelease >> obtainReleaseHandleErrors).unsafeRunSync

    IO(ExitCode.Success)
  }

}

