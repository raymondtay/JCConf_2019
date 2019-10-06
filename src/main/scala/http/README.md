### Resources

The two implementations in this package is to give you an idea how you might
use the `Resource` data type in `http4s` and `akka-http`. If you have used
`http4s` before, you would noticed that it follows the IO Monad ideas very
closely and apparently, that was intentional. The other example (i.e.
`akka-http`) is to give you an idea how you might orientate yourself to
implementing the `Resource` data type.

The key insight i have discovered is that once i found the _right_ abstraction
to encapsulate, the actual use of it (i.e. `resource.use(...)`) reflects the
usual monadic actions (e.g. traversal and error handling); this is particularly
true when it comes to surfacing parallelism i.e. `parTraverse(...)`.

