import java.time.Instant
import scala.math.*

object Vehicle {
  case class Id(value: String) extends AnyVal
  case class LineName(value: String) extends AnyVal
}

case class Vehicle(
    lineName: Vehicle.LineName,
    measuredAt: Instant,
    position: Position,
    id: Vehicle.Id
) {

  def distance(other: Vehicle): Double = {

    val earthRadius = 6371000 // Earth's radius in meters

    val lat1 = toRadians(position.latitude)
    val lon1 = toRadians(position.longitude)
    val lat2 = toRadians(other.position.latitude)
    val lon2 = toRadians(other.position.longitude)

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1

    val a =
      pow(sin(dlat / 2), 2) + cos(lat1) * cos(lat2) * pow(sin(dlon / 2), 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    val distance = earthRadius * c

    distance
  }
}

case class Position(latitude: Double, longitude: Double)

extension (snapshot: Seq[Vehicle]) {
  def join(snapshot2: Seq[Vehicle]): Seq[(Vehicle, Vehicle)] =
    snapshot.flatMap { v1 =>
      snapshot2.collect {
        case v2 if v2.id == v1.id => (v1, v2)
      }
    }
}
