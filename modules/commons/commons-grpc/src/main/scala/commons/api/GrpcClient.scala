package es.eriktorr
package commons.api

import commons.application.GrpcConfig

import cats.effect.{IO, Resource}
import fs2.grpc.syntax.all.given
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

object GrpcClient:
  def managedChannelResource(grpcConfig: GrpcConfig): Resource[IO, ManagedChannel] =
    NettyChannelBuilder
      .forAddress(grpcConfig.host.toString, grpcConfig.port.value)
      .resource[IO]
