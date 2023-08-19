//> using toolkit typelevel:latest

import fs2.*
import cats.effect.IO
import cats.effect.IOApp

val stream: Stream[IO, Int] = 
  Stream
    .iterate(1)(_ + 1)  // Create an infinite stream of natural numbers
    .filter(_ % 2 != 0) // Filter for odd numbers: 1, 3, 5, 7, 9, ...
    .sliding(3)         // Group into 3 element tuples: (1, 3, 5), (7, 9, 11), ...
    .map { chunk => 
      chunk(0) + chunk(1) + chunk(2) // Add them together: 9, 15, 21, ...
    }
    .take(10)           // Fetch the first ten results

object Main extends IOApp.Simple {
  def run: IO[Unit] = stream.compile.toList.flatMap(results => IO(println(results)))
}