package es.eriktorr
package clothing

import clothing.ProtobufWires.given
import clothing.protobuf.{ClothingFs2Grpc, ClothingRequest}
import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig

import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.Stream
import io.grpc.Metadata

trait ClothingClient:
  def findGarmentsBy(request: ClothingRequest): IO[List[Garment]]

object ClothingClient:
  final class Grpc(grpcConfig: GrpcConfig) extends ClothingClient:
    override def findGarmentsBy(request: ClothingRequest): IO[List[Garment]] =
      resource.use: service =>
        (for
          response <- service.sendClothingStream(
            Stream.emit(request).covary[IO],
            Metadata(),
          )
          garments = response.garments.toList.unWire
        yield garments).compile.lastOrError

    private def resource: Resource[IO, ClothingFs2Grpc[IO, Metadata]] =
      GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => ClothingFs2Grpc.stubResource[IO](channel))
