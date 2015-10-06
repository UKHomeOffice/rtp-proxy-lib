package uk.gov.homeoffice.rtp.proxy

import java.net.URL
import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{TrustManagerFactory, KeyManagerFactory, SSLContext}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.Http.ClientConnectionType
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse}
import spray.io.ServerSSLEngineProvider
import spray.routing._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.springframework.util.ResourceUtils
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.gov.homeoffice.resource.CloseableResource._
import scala.collection.JavaConversions._

trait Proxying extends SSLConfiguration {
  val proxy: ProxiedServer => Server => ActorSystem => Any = proxiedServer => server => implicit system => {
    implicit val timeout: Timeout = Timeout(5 seconds)

    val proxiedServerConnectorSetup = Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
                                                              connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port), sslEncryption = true)

    IO(Http)(system) ask proxiedServerConnectorSetup map {
      case Http.HostConnectorInfo(connector, _) =>
        val service = system.actorOf(Props(new ProxyService(connector)))
        IO(Http) ! Http.Bind(service, server.host, server.port)(sslEngineProvider)
    }
  }
}

class ProxyService(val connector: ActorRef) extends HttpServiceActor with ProxyServiceRoute {
  def receive: Receive = runRoute(route)
}

trait ProxyServiceRoute extends Directives {
  implicit val timeout: Timeout = Timeout(5 seconds)

  /*val serverRoute: Route = pathPrefix("proxy-server") {
    pathEndOrSingleSlash {
      get {
        complete {
          HttpEntity(`application/json`, pretty(render("status" -> "I am here!")))
        }
      }
    }
  }*/

  val proxiedServerRoute: Route = (ctx: RequestContext) => ctx.complete {
    connector.ask(ctx.request).mapTo[HttpResponse]
  }

  val route: Route = /*serverRoute ~*/ proxiedServerRoute

  def connector: ActorRef
}

trait SSLConfiguration extends Logging {
  def config: Config

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