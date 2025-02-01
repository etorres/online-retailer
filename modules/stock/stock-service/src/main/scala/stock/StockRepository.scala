package es.eriktorr
package stock

import commons.query.Row.unRow
import commons.query.{Filter, Sort}
import stock.db.StockAvailabilityTable

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import fs2.Stream

trait StockRepository:
  def selectStockAvailabilitiesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int = StockRepository.defaultChunkSize,
  ): Stream[IO, StockAvailability]

object StockRepository:
  private val defaultChunkSize = doobie.util.query.DefaultChunkSize

  final class Postgres(transactor: HikariTransactor[IO]) extends StockRepository:
    override def selectStockAvailabilitiesBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int = StockRepository.defaultChunkSize,
    ): Stream[IO, StockAvailability] =
      val connection = StockAvailabilityTable.selectStockAvailabilitiesBy(filter, sort, chunkSize)
      connection.transact(transactor).map(_.unRow)
