## IO Monads in Scala

![Cats Friendly Badge](https://typelevel.org/cats/img/cats-badge-tiny.png)

This scratchpad is what i used for my talk on the `IO` Monad in Scala delivered in Taipei (Taiwan) @ October 2019.


### Motivation

To understand the `IO` monad, i thought this passage from the library's
[site](https://typelevel.org/cats-effect/datatypes/io.html#introduction) was particularly illuminating.

```
A value of type IO[A] is a computation which, when evaluated, can perform effects before returning a value of type A.

IO values are pure, immutable values and thus preserves referential transparency, being usable in functional programming. An IO is a data structure that represents just a description of a side effectful computation.

IO can describe synchronous or asynchronous computations that:

on evaluation yield exactly one result
can end in either success or failure and in case of failure flatMap chains get short-circuited (IO implementing the algebra of MonadError)
can be canceled, but note this capability relies on the user to provide cancellation logic
Effects described via this abstraction are not evaluated until the “end of the world”, which is to say, when one of the “unsafe” methods are used. Effectful results are not memoized, meaning that memory overhead is minimal (and no leaks), and also that a single effect may be run multiple times in a referentially-transparent manner
...
```

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
