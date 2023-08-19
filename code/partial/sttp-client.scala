//> using dep "com.softwaremill.sttp.client3::fs2:3.8.16"
//> using dep "com.softwaremill.sttp.client3::circe:3.8.16"
//> using dep "com.softwaremill.sttp.client3::core:3.8.16"
//> using toolkit typelevel:latest

import cats.effect.*
import cats.implicits.given
import io.circe.Codec
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.model.MediaType

val apiUri = uri"https://mpk.wroc.pl/bus_position"
val trams = List("31", "33")
val buses = List("110")

def payload(buses: List[String], trams: List[String]) = 
  (trams.map(v => s"busList[tram][]=$v") ++ buses.map(v => s"busList[bus][]=$v")).mkString("&")

case class Record(
  name: String,
  x: Double,
  y: Double,
  k: Int
) derives Codec.AsObject      // This means the compiler will generate JSON Encoder and Decoder 


def request(backend: SttpBackend[IO, Any]): IO[List[Record]] = 
  basicRequest
    .post(apiUri)
    .body(payload(buses, trams)) // Something like busList[bus][]=110&busList[tram][]=31&busList[tram][]=33
    .contentType(MediaType.ApplicationXWwwFormUrlencoded)
    .response(asJson[List[Record]])
    .send(backend)
    .map(_.body)                 // We are only interested in the result
    .rethrow                     // Fail `IO` on all errors, we are being simple here

object Main extends IOApp.Simple {
  def run = 
    HttpClientFs2Backend
      .resource[IO]()
      .use(backend => request(backend))
      .flatMap(IO.println)
}