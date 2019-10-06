### Asynchronous & Cancellable

There are 4 examples here, whose purpose i explain here:

- [[Fib.scala]] is to remind developers that you have to be mindful of what you
  are doing in addition to the "follow the types" concept.
- [[FlatMaps.scala]] & [[Snatch.scala]] is to remind developers that cancellation
  will be invoked for all losers of a _race_; this is a guarantee given by
  `cats-effect` which means that you eliminate resource leaks
- [[MutualRecursion.scala]] is to give developers how one might describe a
  _mutually-recursive_ computation


