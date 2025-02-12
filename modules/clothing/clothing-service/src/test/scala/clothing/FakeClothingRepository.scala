package es.eriktorr
package clothing

import clothing.FakeClothingRepository.ClothingRepositoryState
import commons.query.{Filter, Sort}

import cats.data.OptionT
import cats.effect.{IO, Ref}
import fs2.Stream

final class FakeClothingRepository(stateRef: Ref[IO, ClothingRepositoryState])
    extends ClothingRepository:
  override def findGarmentBy(id: Garment.Id): OptionT[IO, Garment] = OptionT(
    stateRef.get.map(_.garments.find(_.id == id)),
  )

  override def selectGarmentsBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[IO, Garment] = Stream.evals(stateRef.get.map(_.garments))

object FakeClothingRepository:
  final case class ClothingRepositoryState(garments: List[Garment]):
    def set(newGarments: List[Garment]): ClothingRepositoryState = copy(newGarments)

  object ClothingRepositoryState:
    val empty: ClothingRepositoryState = ClothingRepositoryState(List.empty)
