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


trait WroclawOpenDataClient[F[_]] {
  def vehicles(): F[Seq[WroclawOpenDataClient.Record]]
}

object WroclawOpenDataClient {

  def apply[F[_]](using ev: WroclawOpenDataClient[F]): WroclawOpenDataClient[F] = ev

  private def apiUri(limit: Int) = uri"https://www.wroclaw.pl/open-data/api/action/datastore_search?resource_id=17308285-3977-42f7-81b7-fdd168c210a2&limit=$limit"

  def instance(backend: SttpBackend[IO, Any], limit: Int = 5000): WroclawOpenDataClient[IO] = 
    new WroclawOpenDataClient[IO] {
      def vehicles(): IO[Seq[WroclawOpenDataClient.Record]] = 
        basicRequest
          .get(apiUri(limit))
          .response(asJson[Response])
          .send(backend)
          .map(_.body)
          .rethrow
          .map(_.result.records)
    }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
  private given Decoder[Instant] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(LocalDateTime.parse(str, formatter).atZone(ZoneId.systemDefault()).toInstant)
      .leftMap(_ => "Invalid datetime format")
  }

  case class Record(
    Ostatnia_Pozycja_Dlugosc: Double,
    _id: Int,
    Nazwa_Linii: String,
    Brygada: String,
    Data_Aktualizacji: Instant,
    Nr_Boczny: String,
    Ostatnia_Pozycja_Szerokosc: Double
  ) derives Codec.AsObject

  case class Response(success: Boolean, result: Result) derives Codec.AsObject
  case class Result(records: Seq[Record]) derives Codec.AsObject


}
