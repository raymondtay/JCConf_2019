## IO Monads in Scala

![Cats Friendly Badge](https://typelevel.org/cats/img/cats-badge-tiny.png)

This repository is what i used for my talk on the [IO Monad](https://typelevel.org/cats-effect/datatypes/io.html#introduction) in Scala delivered in Taipei (Taiwan) @ October 2019. You can see the recording of my talk [here](https://www.youtube.com/watch?v=qiZhmdatf98).

For this talk, i have prepared 18 code examples where i hope to impress the
readers (thats you ☺) that the IO monad should be your de-facto approach to
solving your problems when using the functional programming libraries like
[Cats](https://typelevel.org/cats).

### Code Examples

There are largely 3 categories of code snippets and its broken down as follows:

* Concurrent `IO` code which are either cancellable or not;
  * Examples that illustrate how to describe asynchronous, concurrent code
    which can describe _error-handling_;
* Concurrent modelling of data structures;
  * _Buffered Channel_ and _Skip Channel_ data structures are introduced and
    implemented; these structures were lifted from the paper _"Concurrent
    Haskell"
* Applications of the `IO Monad`;
  * `Writer Monad` integrated in an computation (e.g. `Fibonacci sequence`) and
     introducing the [log4cats](https://github.com/ChristopherDavenport/log4cats);
     the motivation is to demonstrate how the FP abstractions in
     [cats](https://typelevel.org/cats) integrate nicely into `IO Monad`
  * Modeled the `Resource` _acquire-release_ mechanism around the _http 1.x_
    client provided by [akka-http](https://doc.akka.io/docs/akka-http/current/index.html) and readers can contrast
    the approach taken by [http4s](https://http4s.org) to see how _regular_
    things can be modeled into this approach


### Run

You would need a JDK version 8 (at least) to run these examples; also, you
would want to install [Scala 2.12.x](https://scala-lang.org) and
[sbt](https://scala-sbt.org). 

```
sbt:jcconf_2019> run
[warn] Multiple main classes detected.  Run 'show discoveredMainClasses' to see the list

Multiple main classes detected, select one to run:

 [1] async.cancellable.Beeper
 [2] async.cancellable.RacingNCancellable
 [3] async.cancellable.Sleeper
 [4] async.recovery.Mapper
 [5] cancellable.Fib
 [6] cancellable.FlatMaps
 [7] cancellable.MutualRecursion
 [8] cancellable.Snatcher
 [9] concurrent.BufferedChannel
 [10] concurrent.SkipChannnel
 [11] http.WebCrawlerAkkaHttp
 [12] http.WebCrawlerHttp4s
 [13] logging.FibLogger
 [14] logging.FibWriter
 [15] resources.ResourceAcquisitionRelease
 [16] stack.safe.Fib
 [17] uncancellable.Uncancellable1
 [18] uncancellable.Uncancellable2
[info] Packaging /Users/raymondtay/JCConf_2019/target/scala-2.12/jcconf_2019_2.12-0.1.0-SNAPSHOT.jar ...
[info] Done packaging.

Enter number:
```
