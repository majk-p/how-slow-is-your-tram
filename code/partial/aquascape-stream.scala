//> using scala "3.4.0"
//> using toolkit typelevel:default
//> using dep "com.github.zainab-ali::aquascape:0.1.0"

import fs2.*
import cats.effect.IO
import cats.effect.IOApp

import aquascape.*
import aquascape.given
import doodle.core.*
import doodle.syntax.all.*
import doodle.java2d.*
import cats.effect.unsafe.implicits.global
import doodle.core.format.Png
import cats.Show

given Show[fs2.Chunk[Int]] =
  chunk =>
    s"Chunk(${chunk(0).toString}, ${chunk(1).toString}, ${chunk(2).toString})"

val stream =
  Scape
    .chunked[IO]
    .flatMap { t =>
      given aquascape.Scape[IO] = t

      val stream = Stream
        .iterate[IO, Int](1)(
          _ + 1
        ) // Create an infinite stream of natural numbers
        .stage("Iterate")
        .filter(_ % 2 != 0) // Filter for odd numbers: 1, 3, 5, 7, 9, ...
        .stage("Filter odd")
        .sliding(3) // Group into 3 element tuples: (1, 3, 5), (7, 9, 11), ...
        .stage("Sliding by 3")
        .map { chunk =>
          chunk(0) + chunk(1) + chunk(2) // Add them together: 9, 15, 21, ...
        }
        .stage("Summarize chunks")
        .take(10) // Fetch the first ten results
        .stage("Take 10")

      val output = stream.compile.toList
      val compiledTraced = output.compileStage("My stream")
      val picture = compiledTraced.draw()

      picture.flatMap(_.writeToIO[Png]("/tmp/stream.png")) *> IO.println(
        "Wrote image"
      ) *> compiledTraced
    }
object Main extends IOApp.Simple {
  def run: IO[Unit] =
    stream.flatMap(results => IO(println(results)))
}
