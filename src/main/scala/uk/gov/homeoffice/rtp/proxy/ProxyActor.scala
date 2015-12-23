package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.{ActorLogging, Props}
import akka.event.LoggingReceive
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.http.HttpResponse
import spray.routing.{HttpServiceActor, RequestContext}
import uk.gov.homeoffice.json.Json

object ProxyActor {
  def props(proxiedConnectorSetup: Http.HostConnectorSetup) = Props(new ProxyActor(proxiedConnectorSetup))
}

class ProxyActor(proxiedConnectorSetup: Http.HostConnectorSetup) extends HttpServiceActor with ProxyRoute with Json with ActorLogging {
  def receive: Receive = LoggingReceive {
    runRoute(route)
  }

  def proxy(ctx: RequestContext): Future[HttpResponse] = {
    IO(Http)(context.system) ask proxiedConnectorSetup flatMap {
      case Http.HostConnectorInfo(proxiedConnector, _) =>
        proxiedConnector.ask(ctx.request).mapTo[HttpResponse] map { response =>
          val protocol = response.protocol
          val status = response.status
          val headers = response.headers.map(_.toString()).mkString(", ")
          val entity = response.entity.asString

          info(s"Proxied $protocol response of status $status, headers = $headers, entity = $entity")
          response
        }
    }
  }
}