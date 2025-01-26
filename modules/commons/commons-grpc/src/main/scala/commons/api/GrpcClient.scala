package es.eriktorr
package commons.api

import commons.application.GrpcConfig

import cats.effect.Resource
import cats.effect.kernel.Async
import fs2.grpc.syntax.all.given
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

object GrpcClient:
  def managedChannelResource[F[_]: Async](grpcConfig: GrpcConfig): Resource[F, ManagedChannel] =
    NettyChannelBuilder
      .forAddress(grpcConfig.host.toString, grpcConfig.port.value)
      .resource[F]
