package uk.gov.homeoffice.rtp.proxy

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.console.Console

object BootSSL extends App with Proxying with SSLProxyingConfiguration with HasConfig with Console with Logging {
  present("RTP Proxy Service")

  val proxiedServer = ProxiedServer(config.getString("proxied.server.host"), config.getInt("proxied.server.port"))

  val server = Server(config.getString("spray.can.server.host"), config.getInt("spray.can.server.port"))

  val system = ActorSystem(config.getString("spray.can.server.name"))

  sys.addShutdownHook {
    system.shutdown()
  }

  proxy(proxiedServer)(server)(system)
}