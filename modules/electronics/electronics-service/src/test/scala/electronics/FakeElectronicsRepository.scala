package es.eriktorr
package electronics

import commons.query.{Filter, Sort}
import electronics.FakeElectronicsRepository.ElectronicsRepositoryState

import cats.data.OptionT
import cats.effect.{IO, Ref}
import fs2.Stream

final class FakeElectronicsRepository(stateRef: Ref[IO, ElectronicsRepositoryState])
    extends ElectronicsRepository:
  override def findElectronicDeviceBy(id: ElectronicDevice.Id): OptionT[IO, ElectronicDevice] =
    OptionT(
      stateRef.get.map(_.electronicDevices.find(_.id == id)),
    )

  override def selectElectronicDevicesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[IO, ElectronicDevice] = Stream.evals(stateRef.get.map(_.electronicDevices))

object FakeElectronicsRepository:
  final case class ElectronicsRepositoryState(electronicDevices: List[ElectronicDevice]):
    def set(newElectronicDevices: List[ElectronicDevice]): ElectronicsRepositoryState = copy(
      newElectronicDevices,
    )

  object ElectronicsRepositoryState:
    val empty: ElectronicsRepositoryState = ElectronicsRepositoryState(List.empty)
