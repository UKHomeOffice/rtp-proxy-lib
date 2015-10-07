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

trait Proxying {
  this: ProxyingConfiguration =>

  def proxy(proxiedServer: ProxiedServer, server: Server)(implicit system: ActorSystem) = {
    implicit val timeout: Timeout = Timeout(30 seconds)

    val proxyingConnectorSetup = hostConnectorSetup {
      Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
        connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
    }

    IO(Http)(system) ask proxyingConnectorSetup map {
      case Http.HostConnectorInfo(connector, _) =>
        val proxyActor = system.actorOf(Props(new ProxyActor(connector)))
        IO(Http) ! Http.Bind(proxyActor, server.host, server.port)

        sys.addShutdownHook {
          IO(Http) ? Http.CloseAll
        }

        proxyActor
    }
  }
}

trait ProxyingConfiguration {
  val hostConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = h => h
}

class ProxyActor(val connector: ActorRef) extends HttpServiceActor with ProxyRoute {
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
    connector.ask(ctx.request).mapTo[HttpResponse]
  }

  val route: Route = /*serverRoute ~*/ proxiedServerRoute

  def connector: ActorRef
}