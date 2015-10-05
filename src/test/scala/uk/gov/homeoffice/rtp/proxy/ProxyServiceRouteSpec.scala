package uk.gov.homeoffice.rtp.proxy

import akka.actor.Actor
import akka.testkit.TestActorRef
import spray.http.StatusCodes.OK
import spray.http._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.spray.RouteSpecification

class ProxyServiceRouteSpec(implicit ev: ExecutionEnv) extends RouteSpecification with Mockito {
  trait Context extends ActorSystemContext with ProxyServiceRoute {
    /*val connector = mock[ActorRef]
    connector.ask(any) returns Future { HttpResponse(status = OK) }*/

    val connector = TestActorRef {
      new Actor {
        def receive = {
          case _: HttpRequest =>
            sender ! HttpResponse(status = OK)
        }
      }
    }
  }

  "Proxy service" should {
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

/*
case class HttpRequest(method: HttpMethod = HttpMethods.GET,
                       uri: Uri = Uri./,
                       headers: List[HttpHeader] = Nil,
                       entity: HttpEntity = HttpEntity.Empty,
                       protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`)*/
