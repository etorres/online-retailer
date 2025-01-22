package es.eriktorr
package commons.application

import cats.Show
import com.comcast.ip4s.{host, port, Host, Port}

final case class GrpcConfig(host: Host, port: Port)

object GrpcConfig:
  val defaultHost: Host = host"localhost"
  val defaultPort: Port = port"9999"

  given Show[GrpcConfig] =
    import scala.language.unsafeNulls
    Show.show(config => s"""grpc-host: ${config.host},
                           | grpc-port: ${config.port}""".stripMargin.replaceAll("\\R", ""))
