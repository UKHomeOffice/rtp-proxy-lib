package uk.gov.homeoffice.rtp.proxy.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.console.Console
import uk.gov.homeoffice.rtp.proxy.{ProxiedServer, Proxying, Server}

/**
  * To run:
  * sbt "run-main uk.gov.homeoffice.rtp.proxy.example.ExampleBoot"
  * OR
  * sbt run
  * and then choose the appropriate application to boot.
  */
object ExampleBoot extends App with Console {
  present("RTP Proxy Example")

  val config = ConfigFactory.load("application.example.conf")

  val proxiedServer = ProxiedServer(config.getString("proxied.server.host"), config.getInt("proxied.server.port"))

  val server = Server(config.getString("spray.can.server.host"), config.getInt("spray.can.server.port"))

  implicit val system = ActorSystem("rtp-proxy-example-spray-can", config)

  sys.addShutdownHook {
    system.terminate()
  }

  Proxying().proxy(proxiedServer, server)
}