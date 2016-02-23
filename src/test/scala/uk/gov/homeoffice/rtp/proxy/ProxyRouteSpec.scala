package uk.gov.homeoffice.rtp.proxy

import scala.concurrent.Future
import spray.http.StatusCodes.OK
import spray.http._
import spray.routing.RequestContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import uk.gov.homeoffice.spray.RouteSpecification

class ProxyRouteSpec extends Specification with RouteSpecification with Mockito {
  trait Context extends Scope with ProxyRoute {
    def proxy(ctx: RequestContext): Future[HttpResponse] = Future.successful(HttpResponse(status = OK))
  }

  "Proxy route" should {
    "proxy a GET" in new Context {
      Get("/") ~> route ~> check {
        status mustEqual OK
      }
    }

    "proxy a POST" in new Context {
      Post("/", "Some Data") ~> route ~> check {
        status mustEqual OK
      }
    }
  }
}