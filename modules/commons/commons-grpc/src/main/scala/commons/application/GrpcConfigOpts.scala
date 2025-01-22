package es.eriktorr
package commons.application

import commons.application.GrpcArgument.given

import cats.implicits.catsSyntaxTuple2Semigroupal
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Opts

object GrpcConfigOpts:
  def grpcConfigOpts: Opts[GrpcConfig] = (
    Opts
      .env[Host](
        name = "ONLINE_RETAILER_GRPC_HOST",
        help = "Set gRPC host.",
      )
      .withDefault(GrpcConfig.defaultHost),
    Opts
      .env[Port](
        name = "ONLINE_RETAILER_GRPC_PORT",
        help = "Set gRPC port.",
      )
      .withDefault(GrpcConfig.defaultPort),
  ).mapN(GrpcConfig.apply)
