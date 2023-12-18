import cats.effect.*
import cats.implicits.given
import java.time.Instant
import scala.concurrent.duration.*
import cats.kernel.Monoid
import Vehicle.Id
import Vehicle.LineName
import cats.syntax.group

object StatsCalculator {

  case class VehiclePositionDiff(
      line: Vehicle.LineName,
      id: Vehicle.Id,
      secondsDuration: Double,
      metersDistance: Double
  )

  case class VehicleStats(metersDistance: Double, secondsDuration: Double) {
    val avgSpeedKMH: Double = metersDistance / secondsDuration * 3.6
    override def toString(): String =
      s"VehicleStats(distance = ${metersDistance.round} m, duration = ${secondsDuration.round} s, avgSpeed = $avgSpeedKMH km/h)"
  }

  def secondDuration(start: Instant, end: Instant) =
    (end.toEpochMilli() - start.toEpochMilli()).toDouble / 1000

  def calculateDiff(
      snapshot1: Seq[Vehicle],
      snapshot2: Seq[Vehicle]
  ): Seq[VehiclePositionDiff] =
    snapshot1
      .join(snapshot2)
      .map((v1, v2) =>
        VehiclePositionDiff(
          v1.lineName,
          v1.id,
          secondDuration(v1.measuredAt, v2.measuredAt),
          v1.distance(v2)
        )
      )

  def summarize(
      previousSummary: Map[(Vehicle.LineName, Vehicle.Id), VehicleStats],
      nextDiff: Seq[VehiclePositionDiff]
  ): Map[(LineName, Id), VehicleStats] = {
    val currentSummary =
      nextDiff
        .groupMapReduce(d => (d.line, d.id))(diff =>
          VehicleStats(diff.metersDistance, diff.secondsDuration)
        )((a, b) => a)
    Monoid.combine(previousSummary, currentSummary)
  }

  def stats(vehicles: Vehicles[IO])(
      interval: FiniteDuration,
      numberOfSamples: Int
  ): IO[Map[(LineName, Id), VehicleStats]] =
    fs2.Stream
      .fixedRateStartImmediately[IO](interval)
      .zipWithIndex
      .evalMap((_, idx) =>
        IO.println(
          s"Fetching vehicles ${(idx / numberOfSamples.toDouble * 100).round}%"
        )
      )
      .evalMap(_ => vehicles.list())
      .sliding(2)
      .map(chunk => calculateDiff(chunk(0), chunk(1)))
      .take(numberOfSamples)
      .fold(Map.empty)(summarize)
      .compile
      .toList
      .map(_.head)

  given Monoid[VehicleStats] with {

    override def combine(x: VehicleStats, y: VehicleStats): VehicleStats =
      VehicleStats(
        x.metersDistance + y.metersDistance,
        x.secondsDuration + y.secondsDuration
      )

    override def empty: VehicleStats = VehicleStats(0, 0)

  }

  def aggregateLines(
      summary: Map[(LineName, Id), VehicleStats]
  ): Map[LineName, VehicleStats] =
    summary
      .groupBy { case ((lineName, _), _) => lineName }
      .map((lineName, aggregate) =>
        lineName -> aggregate.values.toList.combineAll
      )

}
