package uk.gov.homeoffice.rtp.proxy.ssl

import java.net.URL
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, TrustManagerFactory, SSLContext}
import org.springframework.util.ResourceUtils
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.gov.homeoffice.resource.CloseableResource._
import scala.collection.JavaConversions._

object SSL extends Logging {
  def loadKeystore(keystoreType: String, keystorePath: URL, keystorePassword: String): KeyStore =
    using(keystorePath.openStream()) { keystoreInputStream =>
      val keystore = KeyStore.getInstance(keystoreType)

      keystore.load(keystoreInputStream, keystorePassword.toCharArray)
      info(s"Loaded keystore of type '$keystoreType' from $keystorePath")
      keystore.aliases().toSeq foreach { a => info(s"Keystore alias: $a") }
      keystore
    }

  def sslContext(config: Config): SSLContext = {
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
}