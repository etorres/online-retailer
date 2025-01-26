package es.eriktorr
package products.application

import com.comcast.ip4s.{port, Port}
import zio.Config

final case class GraphqlConfig(port: Port)

object GraphqlConfig:
  given config: Config[GraphqlConfig] =
    for
      maybePort <- Config.Optional(Config.int("ONLINE_RETAILER_GRAPHQL_PORT"))
      port = maybePort.flatMap(Port.fromInt).getOrElse(port"8090")
    yield GraphqlConfig(port)
