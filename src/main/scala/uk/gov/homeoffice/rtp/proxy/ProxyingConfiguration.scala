package uk.gov.homeoffice.rtp.proxy

import spray.can.Http
import spray.can.Http.ClientConnectionType

trait ProxyingConfiguration {
  val hostConnectorSetup: Http.HostConnectorSetup => Http.HostConnectorSetup = h => h
  
  val proxyingConnectorSetup: ProxiedServer => Http.HostConnectorSetup =
    proxiedServer => hostConnectorSetup {
      Http.HostConnectorSetup(proxiedServer.host, proxiedServer.port,
        connectionType = ClientConnectionType.Proxied(proxiedServer.host, proxiedServer.port))
    }
}