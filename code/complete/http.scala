import cats.effect.IO

import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object http {
  def backend = {
    val httpClient: HttpClient =
      HttpClient.newBuilder().sslContext(hackySslContext).build()
    HttpClientFs2Backend.resourceUsingClient[IO](httpClient)
  }

  // ssl context that trusts everyone since MPK also broke their SSL 🙄
  private val hackySslContext = {
    val trustAllCerts: X509TrustManager = new X509TrustManager() {
      def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
      override def checkServerTrusted(
          x509Certificates: Array[X509Certificate],
          s: String
      ): Unit = ()
      override def checkClientTrusted(
          x509Certificates: Array[X509Certificate],
          s: String
      ): Unit = ()
    }

    val ssl: SSLContext = SSLContext.getInstance("TLS")
    ssl.init(Array.empty, Array(trustAllCerts), new SecureRandom)
    ssl
  }
}
