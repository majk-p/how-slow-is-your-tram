import cats.effect.IO

import sttp.client4.curl.CurlBackend
import sttp.client4.curl.CurlTryBackend
import sttp.client4.Backend
import sttp.client4.impl.cats.CatsMonadAsyncError

import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import cats.effect.kernel.Resource
import scala.util.Try
import sttp.monad.MonadError
import sttp.capabilities.Effect
import sttp.client4.GenericRequest
import sttp.client4.Response
import sttp.client4.curl.AbstractSyncCurlBackend
import sttp.monad.MonadAsyncError
import sttp.client4.Backend
import sttp.client4.wrappers.FollowRedirectsBackend
import sttp.client4.wrappers.DelegateBackend
import sttp.client4.impl.cats.CurlCatsBackend
import sttp.client4.curl.Modifiers
import sttp.client4.curl.internal.CurlOption

object http {

  // private class CurlIOBackend(verbose: Boolean)
  //     extends AbstractSyncCurlBackend(CatsMonadAsyncError[IO], verbose)
  //     with Backend[IO] {}

  private val mods = Modifiers.empty
    .and(CurlOption.SslVerifyPeer, 0)
    .and(CurlOption.SslVerifyHost, 0)
    .and(CurlOption.SslVerifystatus, 0)

  def curlIOBackend = CurlCatsBackend[IO](true, mods)

  def ioBackend: Resource[IO, Backend[IO]] =
    Resource.pure(curlIOBackend)

  def tryBackend: Resource[IO, Backend[Try]] =
    Resource.pure(CurlTryBackend(true, mods))

  // def backend = {
  //   val httpClient: HttpClient =
  //     HttpClient.newBuilder().sslContext(hackySslContext).build()
  //   HttpClientFs2Backend.resourceUsingClient[IO](httpClient)
  // }

  // // ssl context that trusts everyone since MPK also broke their SSL 🙄
  // private val hackySslContext = {
  //   val trustAllCerts: X509TrustManager = new X509TrustManager() {
  //     def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
  //     override def checkServerTrusted(
  //         x509Certificates: Array[X509Certificate],
  //         s: String
  //     ): Unit = ()
  //     override def checkClientTrusted(
  //         x509Certificates: Array[X509Certificate],
  //         s: String
  //     ): Unit = ()
  //   }

  //   val ssl: SSLContext = SSLContext.getInstance("TLS")
  //   ssl.init(Array.empty, Array(trustAllCerts), new SecureRandom)
  //   ssl
  // }
}
