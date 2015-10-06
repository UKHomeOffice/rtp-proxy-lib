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
  val proxy: ProxiedServer => Server => ActorSystem => Any = proxiedServer => server => implicit system => {
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

  val serverRoute: Route = pathPrefix("proxy-server") {
    pathEndOrSingleSlash {
      get {
        complete {
          HttpEntity(`application/json`, pretty(render("status" -> "I am here!")))
        }
      }
    }
  }

  val proxiedServerRoute: Route = (ctx: RequestContext) => ctx.complete {
    connector.ask(ctx.request).mapTo[HttpResponse]
  }

  val route: Route = serverRoute ~ proxiedServerRoute

  def connector: ActorRef
}