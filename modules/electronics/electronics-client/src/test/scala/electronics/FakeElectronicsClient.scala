package es.eriktorr
package electronics

import electronics.FakeElectronicsClient.ElectronicsClientState
import electronics.protobuf.ElectronicsRequest

import cats.effect.{IO, Ref}

final class FakeElectronicsClient(stateRef: Ref[IO, ElectronicsClientState])
    extends ElectronicsClient[IO]:
  override def findGarmentsBy(request: ElectronicsRequest): IO[List[ElectronicDevice]] =
    stateRef.get.map(_.electronicDevices)

object FakeElectronicsClient:
  final case class ElectronicsClientState(electronicDevices: List[ElectronicDevice]):
    def set(newElectronicDevices: List[ElectronicDevice]): ElectronicsClientState = copy(
      newElectronicDevices,
    )

  object ElectronicsClientState:
    val empty: ElectronicsClientState = ElectronicsClientState(List.empty)
