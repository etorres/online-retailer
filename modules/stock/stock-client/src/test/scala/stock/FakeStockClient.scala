package es.eriktorr
package stock

import stock.FakeStockClient.StockClientState
import stock.protobuf.StockRequest

import cats.effect.{IO, Ref}

final class FakeStockClient(stateRef: Ref[IO, StockClientState]) extends StockClient:
  override def findStockAvailabilitiesBy(request: StockRequest): IO[List[StockAvailability]] =
    stateRef.get.map(_.stockAvailabilities)

object FakeStockClient:
  final case class StockClientState(stockAvailabilities: List[StockAvailability]):
    def set(newStockAvailabilities: List[StockAvailability]): StockClientState = copy(
      newStockAvailabilities,
    )

  object StockClientState:
    val empty: StockClientState = StockClientState(List.empty)
