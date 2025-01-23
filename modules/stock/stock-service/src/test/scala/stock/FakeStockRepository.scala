package es.eriktorr
package stock

import commons.query.{Filter, Sort}
import stock.FakeStockRepository.StockRepositoryState

import cats.effect.{IO, Ref}
import fs2.Stream

final class FakeStockRepository(stateRef: Ref[IO, StockRepositoryState]) extends StockRepository:
  override def selectStockAvailabilitiesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[IO, StockAvailability] = Stream.evals(stateRef.get.map(_.stockAvailabilities))

object FakeStockRepository:
  final case class StockRepositoryState(stockAvailabilities: List[StockAvailability]):
    def set(newStockAvailabilities: List[StockAvailability]): StockRepositoryState = copy(
      newStockAvailabilities,
    )

  object StockRepositoryState:
    val empty: StockRepositoryState = StockRepositoryState(List.empty)
