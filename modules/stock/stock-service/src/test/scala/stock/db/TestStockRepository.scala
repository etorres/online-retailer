package es.eriktorr
package stock.db

import commons.query.Row.row
import stock.StockAvailability

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestStockRepository(transactor: HikariTransactor[IO]):
  def add(stockAvailability: StockAvailability): IO[Unit] =
    val connection = StockAvailabilityTable.Impl.insert(stockAvailability.row)
    connection.transact(transactor).void
