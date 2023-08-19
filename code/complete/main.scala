//> using dep "com.softwaremill.sttp.client3::fs2:3.8.16"
//> using dep "com.softwaremill.sttp.client3::circe:3.8.16"
//> using dep "com.softwaremill.sttp.client3::core:3.8.16"
//> using toolkit typelevel:latest
//> using file "WroclawOpenDataClient.scala"
//> using file "MpkWrocApiClient.scala"
//> using file "Vehicles.scala"
//> using file "StatsCalculator.scala"

import Vehicles.Vehicle
import cats.effect.*
import cats.implicits.given
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import java.time.Instant
import scala.concurrent.duration.*
import cats.kernel.Monoid


object Main extends IOApp.Simple {
  def run = 
    HttpClientFs2Backend
      .resource[IO]()
      .use(backend => program(backend) *> IO.println("Program finished"))

  private val trams = List("8", "16", "18", "20", "31", "33")
  private val buses = List("110", "124", "145", "149")


  def program(backend: SttpBackend[IO, Any]) = for {
    _ <- IO.println("Initializing client")
    client = MpkWrocApiClient.instance(backend)(buses, trams)
    vehicles = Vehicles.mpkApiAdapter(client)
    stats <- StatsCalculator.stats(vehicles)
    _ <- IO.println("-"*90)
    _ <- IO.println(stats.mkString("\n"))
    aggregate = StatsCalculator.aggregateLines(stats)
    _ <- IO.println("="*90)
    _ <- IO.println(aggregate.mkString("\n"))
  } yield ()


}
