package es.eriktorr
package clothing

import clothing.ProtobufWires.given
import clothing.protobuf.{ClothingFs2Grpc, ClothingRequest}
import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig

import cats.effect.{Async, Resource}
import fs2.Stream
import io.grpc.Metadata

trait ClothingClient[F[_]: Async]:
  def findGarmentsBy(request: ClothingRequest): F[List[Garment]]

object ClothingClient:
  final class Grpc[F[_]: Async](clothingStub: ClothingFs2Grpc[F, Metadata])
      extends ClothingClient[F]:
    override def findGarmentsBy(request: ClothingRequest): F[List[Garment]] =
      (for
        response <- clothingStub.sendClothingStream(
          Stream.emit(request).covary[F],
          Metadata(),
        )
        garments = response.garments.toList.unWire
      yield garments).compile.lastOrError

  def resource[F[_]: Async](grpcConfig: GrpcConfig): Resource[F, Grpc[F]] =
    for
      clothingStub <- GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => ClothingFs2Grpc.stubResource[F](channel))
      clothingClient = Grpc(clothingStub)
    yield clothingClient
