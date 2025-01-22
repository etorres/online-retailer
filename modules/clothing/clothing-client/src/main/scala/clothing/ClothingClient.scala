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

final class ClothingClient(grpcConfig: GrpcConfig):
  def findGarmentsBy(request: Stream[IO, ClothingRequest]): IO[List[Garment]] =
    resource.use: service =>
      (for
        response <- service.sendClothingStream(request, Metadata())
        garments = response.garments.toList.unWire
      yield garments).compile.lastOrError

  private def resource: Resource[IO, ClothingFs2Grpc[IO, Metadata]] =
    GrpcClient
      .managedChannelResource(grpcConfig)
      .flatMap(channel => ClothingFs2Grpc.stubResource[IO](channel))
