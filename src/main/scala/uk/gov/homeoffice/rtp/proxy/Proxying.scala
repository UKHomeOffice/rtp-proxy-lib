package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.Http.ClientConnectionType
import spray.http.HttpResponse
import spray.routing._

trait Proxying {
  val proxy: ActorSystem => Server => ProxiedServer => Any = implicit system => server => proxiedServer => {
    implicit val timeout: Timeout = Timeout(5 seconds)

    val proxiedServerConnectorSetup = Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
                                                              connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))

    IO(Http)(system) ask proxiedServerConnectorSetup map {
      case Http.HostConnectorInfo(connector, _) =>
        val service = system.actorOf(Props(new ProxyService(connector)))
        IO(Http) ! Http.Bind(service, server.host, server.port)
    }
  }
}

class ProxyService(val connector: ActorRef) extends HttpServiceActor with ProxyServiceRoute {
  def receive: Receive = runRoute(route)
}

trait ProxyServiceRoute extends Directives {
  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = (ctx: RequestContext) => ctx.complete {
    connector.ask(ctx.request).mapTo[HttpResponse]
  }

  def connector: ActorRef
}