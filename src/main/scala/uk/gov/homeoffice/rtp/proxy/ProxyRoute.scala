package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse}
import spray.routing._
import uk.gov.homeoffice.configuration.HasConfig

trait ProxyRoute extends Directives with HasConfig {
  implicit val timeout = Timeout(config.duration("proxied.request-timeout", 30 seconds))

  val serverRoute: Route = pathPrefix("proxy-server") {
    pathEndOrSingleSlash {
      get {
        complete {
          HttpEntity(`application/json`, pretty(render("status" -> "I am here!")))
        }
      }
    }
  }

  val proxiedServerRoute: Route = (ctx: RequestContext) => ctx complete proxy(ctx)

  val route: Route = serverRoute ~ proxiedServerRoute

  def proxy(ctx: RequestContext): Future[HttpResponse]
}