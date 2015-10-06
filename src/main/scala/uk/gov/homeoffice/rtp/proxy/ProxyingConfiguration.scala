package uk.gov.homeoffice.rtp.proxy

import java.net.URL
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.collection.JavaConversions._
import spray.can.Http
import spray.can.Http.ClientConnectionType
import spray.io.ServerSSLEngineProvider
import org.springframework.util.ResourceUtils
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.gov.homeoffice.resource.CloseableResource._

trait ProxyingConfiguration {
  val proxiedServerConnectorSetup: ProxiedServer => Http.HostConnectorSetup =
    proxiedServer => Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
      connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
}

trait SSLProxyingConfiguration extends ProxyingConfiguration with Logging {
  def config: Config

  override val proxiedServerConnectorSetup: ProxiedServer => Http.HostConnectorSetup =
    proxiedServer => Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
      connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port),
      sslEncryption = true)

  implicit def sslContext: SSLContext = {
    val keystoreType = config.getString("ssl.keystore.type")
    val keystorePath = ResourceUtils.getURL(config.getString("ssl.keystore.path"))
    val keystorePassword = config.getString("ssl.keystore.password")

    val keystore = loadKeystore(keystoreType, keystorePath, keystorePassword)

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(keystore, keystorePassword.toCharArray)

    val truststoreType = config.getString("ssl.truststore.type")
    val truststorePath = ResourceUtils.getURL(config.getString("ssl.truststore.path"))
    val truststorePassword = config.getString("ssl.truststore.password")

    val truststore = loadKeystore(truststoreType, truststorePath, truststorePassword)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(truststore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

    sslContext
  }

  implicit def sslEngineProvider: ServerSSLEngineProvider = {
    ServerSSLEngineProvider { engine =>
      /*engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA"))
      engine.setEnabledProtocols(Array("SSLv3", "TLSv1"))*/
      engine
    }
  }

  def loadKeystore(keystoreType: String, keystorePath: URL, keystorePassword: String): KeyStore =
    using(keystorePath.openStream()) { keystoreInputStream =>
      val keystore = KeyStore.getInstance(keystoreType)

      keystore.load(keystoreInputStream, keystorePassword.toCharArray)
      info(s"===> Loaded keystore of type '$keystoreType' from $keystorePath")
      keystore.aliases().toSeq foreach { a => info(s"Keystore alias: $a") }
      keystore
    }
}