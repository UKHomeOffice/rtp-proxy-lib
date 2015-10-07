package uk.gov.homeoffice.rtp.proxy.ssl

import javax.net.ssl.SSLContext
import spray.can.Http.HostConnectorSetup
import spray.io.ServerSSLEngineProvider
import grizzled.slf4j.Logging
import uk.gov.homeoffice.rtp.proxy.ProxyingConfiguration

trait SSLProxyingConfiguration extends ProxyingConfiguration with Logging {
  override val hostConnectorSetup: (HostConnectorSetup) => HostConnectorSetup = _.copy(sslEncryption = true)

  implicit def sslContext: SSLContext

  implicit def sslEngineProvider: ServerSSLEngineProvider = {
    ServerSSLEngineProvider { engine =>
      /*engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA"))
      engine.setEnabledProtocols(Array("TLSv1", "TLSv1.1", "TLSv1.2"))*/
      engine
    }
  }
}