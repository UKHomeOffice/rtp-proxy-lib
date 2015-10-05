package uk.gov.homeoffice.rtp.proxy

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.console.Console

object Boot extends App with Proxying with HasConfig with Console with Logging {
  present("RTP Proxy Service")

  val system = ActorSystem(config.getString("spray.can.server.name"))

  sys.addShutdownHook {
    system.shutdown()
  }

  proxy(system) {
    Server(config.getString("spray.can.server.host"), config.getInt("spray.can.server.port"))
  } {
    ProxiedServer(config.getString("proxied.server.host"), config.getInt("proxied.server.port"))
  }
}