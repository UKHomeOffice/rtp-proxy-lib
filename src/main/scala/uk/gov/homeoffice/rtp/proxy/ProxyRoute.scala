package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import grizzled.slf4j.Logging
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse}
import spray.routing._
import uk.gov.homeoffice.configuration.HasConfig

trait ProxyRoute extends Directives with HasConfig with Logging {
  implicit val timeout: Timeout = Timeout(config.duration("proxied.request-timeout", 30 seconds))

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
    val request = ctx.request
    val protocol = request.protocol
    val uri = request.uri
    val method = request.method
    val headers = request.headers.map(_.toString()).mkString(", ")
    val entity = request.entity.asString

    info(s"Proxying $protocol request to $uri, method = $method, headers = $headers, entity = $entity")
    proxy(ctx)
  }

  val route: Route = serverRoute ~ proxiedServerRoute

  def proxy(ctx: RequestContext): Future[HttpResponse]
}