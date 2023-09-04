import Vehicles.Vehicle
import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import io.circe.Decoder
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.math.*

trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}

object Vehicles {

  def apply[F[_]](using ev: Vehicles[F]): Vehicles[F] = ev

  def mpkApiAdapter(client: MpkWrocApiClient[IO]): Vehicles[IO] = 
    new Vehicles[IO] {
      def list(): IO[Seq[Vehicle]] = 
        for {
          now <- IO.realTimeInstant
          records <- client.vehicles()
        } yield {
          records
            .map{  record => 
              Vehicle(
                lineName = Vehicle.LineName(record.name),
                measuredAt = now,
                position = Position(record.x, record.y),
                id = Vehicle.Id(record.k.toString)
              )
            }
        }
    }

  def wroclawOpenDataAdapter(client: WroclawOpenDataClient[IO]): Vehicles[IO] = 
    new Vehicles[IO] {
      def list(): IO[Seq[Vehicle]] = 
        client.vehicles().map{ vehicles =>
          vehicles.map{ record => 
            Vehicle(
              lineName = Vehicle.LineName(record.Nazwa_Linii),
              measuredAt = record.Data_Aktualizacji,
              position = Position(record.Ostatnia_Pozycja_Szerokosc, record.Ostatnia_Pozycja_Dlugosc),
              id = Vehicle.Id(record._id.toString)
            )
          }

        }
    }

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

      val a = pow(sin(dlat / 2), 2) + cos(lat1) * cos(lat2) * pow(sin(dlon / 2), 2)
      val c = 2 * atan2(sqrt(a), sqrt(1 - a))

      val distance = earthRadius * c

      distance
    }
  }

  case class Position(latitude: Double, longitude: Double)



  extension (snapshot: Seq[Vehicle]) {
    def join(snapshot2: Seq[Vehicle]): Seq[(Vehicle, Vehicle)] = 
      snapshot.flatMap{ v1 =>
        snapshot2.collect {
          case v2 if v2.id == v1.id => (v1, v2)
        }  
      }
    }

}

