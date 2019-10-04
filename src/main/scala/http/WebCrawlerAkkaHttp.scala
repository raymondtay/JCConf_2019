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

package http

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

/**
 * The demonstration here is to focus on
 * integrating the Akka http framework with the IO monad and to leverage it
 * as a more general library.
 *
 * The contrast between [[WebCrawlerHttp4s.scala]] is that `http4s` was
 * designed as a FP-styled library hence its more seamless than when compared
 * to the [[akka-http]] framework.
 **/
trait AkkaHttpResource {

  implicit val system           = ActorSystem()
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val cs               = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

  def requestHttp1x(client: HttpExt, site: String) : IO[Unit] = {
    IO.fromFuture(IO.pure[Future[HttpResponse]](client.singleRequest(HttpRequest(uri = site))))
      .flatMap(result => IO(println(result.status)))
      .handleErrorWith(error => IO(println("Error"))) *> IO.unit
  }

  def makeHttp1xResource = {
    val acquire = IO(Http())
    def release(http1x : HttpExt) = IO.unit /* nothing to release in Http1.x */
    Resource.make(acquire)(release)
  }

}

object WebCrawlerAkkaHttp extends IOApp with AkkaHttpResource {

  val sites =
    List(
      "https://en.wikipedia.org/wiki/Spade",
      "https://en.wikipedia.org/wiki/Shovel",
      "https://www.google.com",
      "https://www.bing.com",
      "https://www.yahoo.com",
      "https://reddit.com"
    )

  def run(args: List[String]): IO[ExitCode] = {

    lazy val responses0 : IO[List[Unit]] =
      sites traverse (site => makeHttp1xResource.use(client => requestHttp1x(client, site)))

    lazy val responses1 : List[IO[Unit]] =
      sites.map(site => makeHttp1xResource.use(client => requestHttp1x(client, site)))

    lazy val responses2 : IO[List[Unit]] = {
      import IO._ /* bring in the Parallel[IO] implicit instances */
      sites parTraverse (site => makeHttp1xResource.use(client => requestHttp1x(client, site)))
    }

    // option 1. launches and sleeps for 3 seconds
    // (responses0 *> timer.sleep(3 seconds) *> IO(system.terminate)).unsafeRunSync
    (responses2 *> timer.sleep(3 seconds) *> IO(system.terminate)).unsafeRunSync

    // option 2. launches and sleeps for 3 seconds
    // (responses1.sequence *> timer.sleep(3 seconds) *> IO(system.terminate)).unsafeRunSync

    IO(println("main program terminated")) *>
    IO(ExitCode.Success)
  }

}

