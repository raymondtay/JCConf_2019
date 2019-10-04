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

package object concurrent {

  import cats.effect.{IO,Concurrent}
  import cats.effect.concurrent.MVar

  type Stream[A, F[_]] = MVar[F, Item[A,F]]

  case class Item[A,F[_]](head : A, tail : Stream[A,F])

  // The `MVar`s in a `Channel` are required so that channel put and get
  // operations can atomically modify the write and read end of the channels
  // respectively.
  case class Channel[A, F[_]:Concurrent](
    reader : MVar[F, Stream[A,F]],
    writer : MVar[F, Stream[A,F]]
  )

  case class SkipChan[A, F[_]](main : MVar[F, (A, List[MVar[F,Unit]])], sem: MVar[F, Unit])

}
