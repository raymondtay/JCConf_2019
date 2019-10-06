### Concurrent Modelling

The examples here are translated directly from the _Concurrent Haskell_ paper
by Simon Peyton Jones et al. Why did i choose to translate those ideas from
Haskell to Scala ? Reason was simple: the data types and typeclasses introduced
by [cats-effect](https://typelevel.org/cats-effect) have the same
specifications as defined by this same paper and it seemed like a good idea, at
that time.

A key insight here is that i have the _freedom_ to enable (i.e. invoking
`...start`) _fiber_s on starting the computation when it makes sense; otherwise
we leave it as it is. This might seem unimportant at first but it becomes
important when one is debugging the concurrent data structure.


