//> using toolkit typelevel:latest
//> using dep "com.softwaremill.sttp.client3::fs2:3.9.3"
//> using dep "com.softwaremill.sttp.client3::circe:3.9.3"
//> using dep "com.softwaremill.sttp.client3::core:3.9.3"
//> using dep "com.github.zainab-ali::aquascape:0.0-59dea01-SNAPSHOT"
//> using file "WroclawOpenDataClient.scala"
//> using file "Vehicles.scala"
//> using file "StatsCalculator.scala"

import cats.effect.*
import cats.implicits.given
import cats.kernel.Monoid
import sttp.client3.*
import sttp.client3.circe.*

import java.time.Instant
import scala.concurrent.duration.*

object Main extends IOApp.Simple {

  def run =
    http.backend
      .use(backend => program(backend) *> IO.println("Program finished"))

  val trams = List("8", "16", "18", "20", "21", "22")
  val buses = List("124", "145", "149")
  val interval = 9.seconds
  val numberOfSamples = 72

  def program(backend: SttpBackend[IO, Any]) = for {
    _ <- IO.println("Initializing client")
    vehicles = Vehicles.mpkWrocInstance(backend, buses, trams)
    rawStats <- StatsCalculator.statsTraced(vehicles)(interval, numberOfSamples)
    stats = rawStats.filterNot((_, stats) => stats.avgSpeedKMH > 80)
    _ <- IO.println("-" * 90)
    _ <- IO.println(stats.mkString("\n"))
    aggregate = StatsCalculator.aggregateLines(stats)
    _ <- IO.println("=" * 90)
    _ <- IO.println(aggregate.mkString("\n"))
    fastest = aggregate.maxBy((line, stats) => stats.avgSpeedKMH)
    slowest = aggregate.minBy((line, stats) => stats.avgSpeedKMH)
    avg = aggregate.values.map(_.avgSpeedKMH).reduce((a, b) => (a + b) / 2)
    _ <- IO.println(s"Fastest: $fastest")
    _ <- IO.println(s"Slowest: $slowest")
    _ <- IO.println(s"Average: $avg")
  } yield ()

}
