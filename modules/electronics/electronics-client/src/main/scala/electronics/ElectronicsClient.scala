package es.eriktorr
package electronics

import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig
import electronics.ProtobufWires.given
import electronics.protobuf.{ElectronicsFs2Grpc, ElectronicsRequest}

import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.Stream
import io.grpc.Metadata

final class ElectronicsClient(grpcConfig: GrpcConfig):
  def findGarmentsBy(request: Stream[IO, ElectronicsRequest]): IO[List[ElectronicDevice]] =
    resource.use: service =>
      (for
        response <- service.sendElectronicsStream(request, Metadata())
        garments = response.electronicDevices.toList.unWire
      yield garments).compile.lastOrError

  private def resource: Resource[IO, ElectronicsFs2Grpc[IO, Metadata]] =
    GrpcClient
      .managedChannelResource(grpcConfig)
      .flatMap(channel => ElectronicsFs2Grpc.stubResource[IO](channel))
