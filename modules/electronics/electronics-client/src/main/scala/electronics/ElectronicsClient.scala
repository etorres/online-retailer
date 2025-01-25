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

trait ElectronicsClient:
  def findGarmentsBy(request: ElectronicsRequest): IO[List[ElectronicDevice]]

object ElectronicsClient:
  final class Grpc(grpcConfig: GrpcConfig) extends ElectronicsClient:
    override def findGarmentsBy(request: ElectronicsRequest): IO[List[ElectronicDevice]] =
      resource.use: service =>
        (for
          response <- service.sendElectronicsStream(
            Stream.emit(request).covary[IO],
            Metadata(),
          )
          electronicDevices = response.electronicDevices.toList.unWire
        yield electronicDevices).compile.lastOrError

    private def resource: Resource[IO, ElectronicsFs2Grpc[IO, Metadata]] =
      GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => ElectronicsFs2Grpc.stubResource[IO](channel))
