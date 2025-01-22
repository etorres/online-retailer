package es.eriktorr
package commons.api

import commons.application.GrpcConfig

import cats.effect.{IO, Resource}
import fs2.grpc.syntax.all.given
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.{Server, ServerServiceDefinition}

object GrpcServer:
  def runServer(service: ServerServiceDefinition, grpcConfig: GrpcConfig): Resource[IO, Server] =
    NettyServerBuilder
      .forPort(grpcConfig.port.value)
      .addService(service)
      .resource[IO]
