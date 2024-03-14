import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import io.circe.Decoder
import sttp.client3.SttpBackend
import sttp.client3.basicRequest
import sttp.client3.UriContext
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.model.MediaType

trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}

object Vehicles {

  def apply[F[_]](using ev: Vehicles[F]): Vehicles[F] = ev

  private val apiUri = uri"https://mpk.wroc.pl/bus_position"
  private def request(
      backend: SttpBackend[IO, Any],
      buses: List[String],
      trams: List[String]
  ) =
    basicRequest
      .post(apiUri)
      .body(payload(buses, trams))
      .contentType(MediaType.ApplicationXWwwFormUrlencoded)
      .response(asJson[List[MpkRecord]])
      .send(backend)
      .map(_.body)
      .rethrow
  private def payload(buses: List[String], trams: List[String]) =
    (trams.map(v => s"busList[tram][]=$v") ++
      buses.map(v => s"busList[bus][]=$v")).mkString("&")

  def xInstance(
      backend: SttpBackend[IO, Any],
      buses: List[String],
      trams: List[String]
  ): Vehicles[IO] = { () =>
    (request(backend, buses, trams), IO.realTimeInstant).mapN {
      (responses, now) =>
        responses.map { record =>
          Vehicle(
            lineName = Vehicle.LineName(record.name),
            measuredAt = now,
            position = Position(record.x, record.y),
            id = Vehicle.Id(record.k.toString)
          )
        }
    }
  }

  def mpkWrocInstance(
      backend: SttpBackend[IO, Any],
      buses: List[String],
      trams: List[String]
  ): Vehicles[IO] =
    new Vehicles[IO] {
      private val apiUri = uri"https://mpk.wroc.pl/bus_position"
      def list(): IO[Seq[Vehicle]] =
        for {
          now <- IO.realTimeInstant
          records <- request
          results = records
            .map { record =>
              Vehicle(
                lineName = Vehicle.LineName(record.name),
                measuredAt = now,
                position = Position(record.x, record.y),
                id = Vehicle.Id(record.k.toString)
              )
            }
        } yield results

      private val request =
        basicRequest
          .post(apiUri)
          .body(payload(buses, trams))
          .contentType(MediaType.ApplicationXWwwFormUrlencoded)
          .response(asJson[List[MpkRecord]])
          .send(backend)
          .map(_.body)
          .rethrow

      val x =
        (request, IO.realTimeInstant).mapN { (responses, now) =>
          responses.map { record =>
            Vehicle(
              lineName = Vehicle.LineName(record.name),
              measuredAt = now,
              position = Position(record.x, record.y),
              id = Vehicle.Id(record.k.toString)
            )
          }
        }

      private def payload(buses: List[String], trams: List[String]) =
        (trams.map(v => s"busList[tram][]=$v") ++
          buses.map(v => s"busList[bus][]=$v")).mkString("&")
    }

  case class MpkRecord(
      name: String,
      x: Double,
      y: Double,
      k: Int
  ) derives Codec.AsObject

}
