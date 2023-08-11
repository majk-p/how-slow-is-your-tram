import cats.effect.*
import cats.implicits.given
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import io.circe.Codec
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import io.circe.Decoder
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import sttp.model.MediaType


trait MpkWrocApiClient[F[_]] {
  def vehicles(): F[Seq[MpkWrocApiClient.Record]]
}

object MpkWrocApiClient {

  def apply[F[_]](using ev: MpkWrocApiClient[F]): MpkWrocApiClient[F] = ev

  private val apiUri = uri"https://mpk.wroc.pl/bus_position"
  private val trams = List("16", "18", "31")
  private val buses = List("110")


  def instance(backend: SttpBackend[IO, Any])(buses: List[String], trams: List[String]): MpkWrocApiClient[IO] = 
    new MpkWrocApiClient[IO] {
      def vehicles(): IO[Seq[MpkWrocApiClient.Record]] = 
        basicRequest
          .post(apiUri)
          .body(payload(buses, trams))
          .contentType(MediaType.ApplicationXWwwFormUrlencoded)
          .response(asJson[List[Record]])
          .send(backend)
          .map(_.body)
          .rethrow
    }
  
  private def payload(buses: List[String], trams: List[String]) = 
    (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

  case class Record(
    name: String,
    // `type`: String,
    x: Double,
    y: Double,
    k: Int
  ) derives Codec.AsObject


}
