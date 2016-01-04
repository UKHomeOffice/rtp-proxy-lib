package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.duration._
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.Http.ClientConnectionType
import uk.gov.homeoffice.configuration.HasConfig

object Proxying {
  def apply() = new Proxying()
}

class Proxying private[proxy] () extends HasConfig {
  val customiseProxiedConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = h => h

  def proxy(proxiedServer: ProxiedServer, server: Server)(implicit system: ActorSystem): ActorRef = {
    implicit val timeout: Timeout = Timeout(config.duration("proxied.request-timeout", 30 seconds))

    val proxiedConnectorSetup = customiseProxiedConnectorSetup {
      Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
                              connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
    }

    val proxyActor = system.actorOf(ProxyActor.props(proxiedConnectorSetup), "proxy-actor")
    IO(Http) ! Http.Bind(proxyActor, server.host, server.port)

    sys addShutdownHook {
      IO(Http) ? Http.CloseAll
    }

    proxyActor
  }
}