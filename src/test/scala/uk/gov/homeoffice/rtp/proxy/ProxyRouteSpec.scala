package uk.gov.homeoffice.rtp.proxy

import akka.actor.Actor
import akka.testkit.TestActorRef
import spray.http.StatusCodes.OK
import spray.http._
import org.specs2.mock.Mockito
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.spray.RouteSpecification

class ProxyRouteSpec extends RouteSpecification with Mockito {
  trait Context extends ActorSystemContext with ProxyRoute {
    /*val connector = mock[ActorRef]
    connector.ask(any) returns Future { HttpResponse(status = OK) }*/

    val hostConnector = TestActorRef {
      new Actor {
        def receive = {
          case _: HttpRequest =>
            sender ! HttpResponse(status = OK)
        }
      }
    }
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