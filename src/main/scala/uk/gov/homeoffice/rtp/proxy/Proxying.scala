package uk.gov.homeoffice.rtp.proxy

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
import spray.routing._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object Proxying {
  def apply() = new Proxying()
}

class Proxying private[proxy] () {
  val hostConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = h => h

  def proxy(proxiedServer: ProxiedServer, server: Server)(implicit system: ActorSystem) = {
    implicit val timeout: Timeout = Timeout(30 seconds)

    val proxyingConnectorSetup = hostConnectorSetup {
      Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
                              connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
    }

    IO(Http)(system) ask proxyingConnectorSetup map {
      case Http.HostConnectorInfo(hostConnector, _) =>
        val proxyActor = system.actorOf(Props(new ProxyActor(hostConnector)), "host-connector-actor")
        IO(Http) ! Http.Bind(proxyActor, server.host, server.port)

        sys.addShutdownHook {
          IO(Http) ? Http.CloseAll
        }

        proxyActor
    }
  }
}

class ProxyActor(val hostConnector: ActorRef) extends HttpServiceActor with ProxyRoute {
  def receive: Receive = runRoute(route)
}

trait ProxyRoute extends Directives {
  implicit val timeout: Timeout = Timeout(30 seconds)

  // TODO Issue with "health check" route - it can stop the proxy working correctly for some reason
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
    hostConnector.ask(ctx.request).mapTo[HttpResponse]
  }

  val route: Route = /*serverRoute ~*/ proxiedServerRoute

  def hostConnector: ActorRef
}