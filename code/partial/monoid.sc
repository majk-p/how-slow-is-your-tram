//> using toolkit typelevel:latest

import cats.Monoid

val a = Map[String, Int]("foo" -> 1, "bar" -> 5)
val b = Map[String, Int]("bar" -> 3, "baz" -> 0)

println(
    Monoid.combine(a, b)
)
