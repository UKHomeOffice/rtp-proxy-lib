package uk.gov.homeoffice.rtp.proxy.example

import akka.actor.ActorSystem
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.console.Console
import uk.gov.homeoffice.rtp.proxy.ssl.{SSL, SSLProxying}
import uk.gov.homeoffice.rtp.proxy.{ProxiedServer, Server}

object ExampleBootSSL extends App with HasConfig with Console {
  present("RTP Proxy SSL Example")

  val proxiedServer = ProxiedServer(config.getString("proxied.server.host"), config.getInt("proxied.server.port"))

  val server = Server(config.getString("spray.can.server.host"), config.getInt("spray.can.server.port"))

  implicit val system = ActorSystem(config.getString("spray.can.server.name"))

  sys.addShutdownHook {
    system.shutdown()
  }

  SSLProxying(SSL.sslContext(config)).proxy(proxiedServer, server)
}