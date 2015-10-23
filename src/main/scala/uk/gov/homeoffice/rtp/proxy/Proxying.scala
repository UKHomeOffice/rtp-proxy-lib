package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor._
import akka.event.LoggingReceive
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
import uk.gov.homeoffice.json.Json

object Proxying {
  def apply() = new Proxying()
}

class Proxying private[proxy] () {
  val customiseProxiedConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = h => h

  def proxy(proxiedServer: ProxiedServer, server: Server)(implicit system: ActorSystem): ActorRef = {
    implicit val timeout: Timeout = Timeout(30 seconds)

    val proxiedConnectorSetup = customiseProxiedConnectorSetup {
      Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
                              connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
    }

    val proxyActor = system.actorOf(ProxyActor.props(proxiedConnectorSetup), "proxy-actor")
    IO(Http) ! Http.Bind(proxyActor, server.host, server.port)

    sys.addShutdownHook {
      IO(Http) ? Http.CloseAll
    }

    proxyActor
  }
}

object ProxyActor {
  def props(proxiedConnectorSetup: Http.HostConnectorSetup) = Props(new ProxyActor(proxiedConnectorSetup))
}

class ProxyActor(proxiedConnectorSetup: Http.HostConnectorSetup) extends HttpServiceActor with ProxyRoute with Json with ActorLogging {
  def receive: Receive = LoggingReceive {
    runRoute(route)
  }

  def proxy(ctx: RequestContext): Future[HttpResponse] = {
    log.info(s"Proxying request of URI ${ctx.request.uri}")

    IO(Http)(context.system) ask proxiedConnectorSetup flatMap {
      case Http.HostConnectorInfo(proxiedConnector, _) => proxiedConnector.ask(ctx.request).mapTo[HttpResponse]
    }
  }
}

trait ProxyRoute extends Directives {
  implicit val timeout: Timeout = Timeout(30 seconds)

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
    proxy(ctx)
  }

  val route: Route = serverRoute ~ proxiedServerRoute

  def proxy(ctx: RequestContext): Future[HttpResponse]
}