package es.eriktorr
package electronics

import electronics.FakeElectronicsClient.ElectronicsClientState
import electronics.protobuf.{ElectronicsRequest, GetElectronicDeviceRequest}

import cats.data.OptionT
import cats.effect.{IO, Ref}

final class FakeElectronicsClient(stateRef: Ref[IO, ElectronicsClientState])
    extends ElectronicsClient[IO]:
  override def getElectronicDevice(
      request: GetElectronicDeviceRequest,
  ): OptionT[IO, ElectronicDevice] = OptionT(
    stateRef.get.map(_.electronicDevices.find(_.sku == request.sku)),
  )
  override def findElectronicDevicesBy(request: ElectronicsRequest): IO[List[ElectronicDevice]] =
    stateRef.get.map(_.electronicDevices)

object FakeElectronicsClient:
  final case class ElectronicsClientState(electronicDevices: List[ElectronicDevice]):
    def set(newElectronicDevices: List[ElectronicDevice]): ElectronicsClientState = copy(
      newElectronicDevices,
    )

  object ElectronicsClientState:
    val empty: ElectronicsClientState = ElectronicsClientState(List.empty)
