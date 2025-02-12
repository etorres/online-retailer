package es.eriktorr
package electronics

import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig
import electronics.ProtobufWires.given
import electronics.protobuf.{ElectronicsFs2Grpc, ElectronicsRequest, GetElectronicDeviceRequest}

import cats.data.OptionT
import cats.effect.{Async, Resource}
import fs2.Stream
import io.grpc.Metadata

trait ElectronicsClient[F[_]: Async]:
  def getElectronicDevice(request: GetElectronicDeviceRequest): OptionT[F, ElectronicDevice]
  def findElectronicDevicesBy(request: ElectronicsRequest): F[List[ElectronicDevice]]

object ElectronicsClient:
  final class Grpc[F[_]: Async](electronicsStub: ElectronicsFs2Grpc[F, Metadata])
      extends ElectronicsClient[F]:
    override def getElectronicDevice(
        request: GetElectronicDeviceRequest,
    ): OptionT[F, ElectronicDevice] =
      OptionT((for
        response <- electronicsStub.getElectronicDevice(Stream.emit(request).covary[F], Metadata())
        maybeElectronicDevice = response.electronicDevice.unWire
      yield maybeElectronicDevice).compile.lastOrError)

    override def findElectronicDevicesBy(request: ElectronicsRequest): F[List[ElectronicDevice]] =
      (for
        response <- electronicsStub.sendElectronicsStream(
          Stream.emit(request).covary[F],
          Metadata(),
        )
        electronicDevices = response.electronicDevices.toList.unWire
      yield electronicDevices).compile.lastOrError

  def resource[F[_]: Async](grpcConfig: GrpcConfig): Resource[F, Grpc[F]] =
    for
      electronicsStub <- GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => ElectronicsFs2Grpc.stubResource[F](channel))
      electronicsClient = Grpc(electronicsStub)
    yield electronicsClient
