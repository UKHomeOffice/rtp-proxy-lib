package uk.gov.homeoffice.rtp.proxy.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.console.Console
import uk.gov.homeoffice.rtp.proxy.ssl.SSL._
import uk.gov.homeoffice.rtp.proxy.ssl.SSLProxying
import uk.gov.homeoffice.rtp.proxy.{ProxiedServer, Server}

/**
  * To run:
  * sbt "run-main uk.gov.homeoffice.rtp.proxy.example.ExampleBootSSL"
  * OR
  * sbt run
  * and then choose the appropriate application to boot.
  */
object ExampleBootSSL extends App with Console {
  present("RTP SSL Proxy Example")

  val config = ConfigFactory.load("application.example.ssl.conf")

  val proxiedServer = ProxiedServer(config.getString("proxied.server.host"), config.getInt("proxied.server.port"))

  val server = Server(config.getString("spray.can.server.host"), config.getInt("spray.can.server.port"))

  implicit val system = ActorSystem("rtp-proxy-ssl-example-spray-can", config)

  sys.addShutdownHook {
    system.terminate()
  }

  SSLProxying(sslContext(config)).proxy(proxiedServer, server)
}