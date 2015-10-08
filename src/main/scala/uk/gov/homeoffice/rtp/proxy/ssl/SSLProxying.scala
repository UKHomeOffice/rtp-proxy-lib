package uk.gov.homeoffice.rtp.proxy.ssl

import javax.net.ssl.SSLContext
import spray.can.Http
import spray.io.ServerSSLEngineProvider
import uk.gov.homeoffice.rtp.proxy.Proxying

object SSLProxying {
  def apply(implicit sslContext: SSLContext) = new SSLProxying
}

class SSLProxying private[ssl] (implicit sslContext: SSLContext) extends Proxying {
  override val hostConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = _.copy(sslEncryption = true)

  implicit def sslEngineProvider: ServerSSLEngineProvider = {
    ServerSSLEngineProvider { engine =>
      /*engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA"))
      engine.setEnabledProtocols(Array("TLSv1", "TLSv1.1", "TLSv1.2"))*/
      engine
    }
  }
}