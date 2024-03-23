import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import io.circe.Decoder
import sttp.client4.{Backend => SttpBackend}
import sttp.client4.basicRequest
import sttp.client4.UriContext
import sttp.client4.circe.*
import sttp.model.MediaType
import scala.util.Try

trait Vehicles[F[_]] {
  def list(): F[Seq[Vehicle]]
}

object Vehicles {

  def apply[F[_]](using ev: Vehicles[F]): Vehicles[F] = ev

  def mpkWrocInstance(
      backend: SttpBackend[Try],
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
        IO.fromTry(
          basicRequest
            .post(apiUri)
            .body(payload(buses, trams))
            .contentType(MediaType.ApplicationXWwwFormUrlencoded)
            .response(asJson[List[MpkRecord]])
            .send(backend)
        ).map(_.body)
          .rethrow

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
