import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model.MediaType


trait MpkWrocApiClient[F[_]] {
  def vehicles(): F[Seq[MpkWrocApiClient.Record]]
}

object MpkWrocApiClient {

  def apply[F[_]](using ev: MpkWrocApiClient[F]): MpkWrocApiClient[F] = ev

  private val apiUri = uri"https://mpk.wroc.pl/bus_position"

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
    x: Double,
    y: Double,
    k: Int
  ) derives Codec.AsObject


}
