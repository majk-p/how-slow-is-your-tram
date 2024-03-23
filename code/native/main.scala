//> using platform native
//> using toolkit typelevel:latest
//> using dep "com.softwaremill.sttp.client4::fs2::3.3.18+1747-c417ba1d+20231231-1442-SNAPSHOT"
//> using dep "com.softwaremill.sttp.client4::circe::3.3.18+1747-c417ba1d+20231231-1442-SNAPSHOT"
//> using dep "com.softwaremill.sttp.client4::core::3.3.18+1747-c417ba1d+20231231-1442-SNAPSHOT"

//> using file "Vehicles.scala"
//> using file "StatsCalculator.scala"

/** links useful later: https://github.com/MasseGuillaume/hands-on-scala-native
  * https://masseguillaume.github.io/hands-on-scala-native-slides/#/9/1
  *
  * Implementation notes: needs libcurl-devel, libidn-devel, libidn2-devel
  *
  * as per
  * http://www.sslchecker.com/sslchecker?su=a402c9eabc1164b50dc47451f0e09a61 it
  * looks like mpk.wroc.pl cert is broken - it is missing the intermediate CA
  * cert Browsers seem to deal with that
  * https://security.stackexchange.com/questions/220736/understanding-how-clients-handle-incomplete-certificate-chains
  * but curl backend doesn't do AIA nor allows skipping ssl
  */

import cats.effect.*
import cats.implicits.given
import cats.kernel.Monoid
import sttp.client4.*
import sttp.client4.circe.*

import java.time.Instant
import scala.concurrent.duration.*
import scala.util.Try

object Main extends IOApp.Simple {
  def run =
    http.tryBackend
      .use(backend => program(backend) *> IO.println("Program finished"))

  val trams = List("8", "16", "18", "20", "21", "22")
  val buses = List("124", "145", "149")
  val interval = 9.seconds
  val numberOfSamples = 5

  def program(backend: Backend[Try]) = for {
    _ <- IO.println("Initializing client")
    vehicles = Vehicles.mpkWrocInstance(backend, buses, trams)
    rawStats <- StatsCalculator.stats(vehicles)(interval, numberOfSamples)
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
