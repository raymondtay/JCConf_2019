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

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.client.blaze._
import org.http4s.client._

import scala.concurrent.ExecutionContext

/**
 * A web crawler, built from [http4s](http://http4s.org/), when combined with
 * cats-effect allows us to design simple forking concurrent logic to crawl the
 * sites sequentially or in parallel.
 *
 * Caveat: http4s was developed alongside cats-effect (i.e. IO monad) so
 * there's a good reason why they tend to like one another.
 *
 * @author Raymond Tay
 * @version 1.0
 */
object WebCrawlerHttp4s extends IOApp {

  val sites =
    List(
      "https://en.wikipedia.org/wiki/Spade",
      "https://en.wikipedia.org/wiki/Shovel",
      "https://www.google.com",
      "https://www.bing.com",
      "https://www.yahoo.com",
      "https://reddit.com"
    )

  def crawlSite(site: String) =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client =>
      IO(println(s"[Thread-${Thread.currentThread.getName}] Fetching:[$site]")) *>
      (middleware.FollowRedirect(5)(client)).expect[String](site)
    }

  def run(args: List[String]) : IO[ExitCode] = {

    // sequential evaluation
    def crawlAll : List[Int] =
      sites.map(site => Resource.liftF(IO.pure(site)).use(crawlSite).unsafeRunSync.size)

    // parallel evaluation via declaration
    def crawlAll2 : IO[List[String]] =
      sites.parTraverse(site => Resource.liftF(IO.pure(site)).use(crawlSite))

    // sequential evaluation
    def crawlAll3 : List[IO[String]] =
      sites.map(site => Monad[Id].pure(site) >>= crawlSite)

    println(crawlAll)
    println(crawlAll2.unsafeRunSync.map(_.size))
    println(crawlAll3.parSequence.unsafeRunSync.map(_.size))

    IO(ExitCode.Success)
  }

}
