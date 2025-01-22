package es.eriktorr
package clothing

import clothing.db.Garment.given
import clothing.db.GarmentConnection
import commons.query.Row.unRow
import commons.query.{Filter, Sort}

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given
import fs2.Stream

trait ClothingRepository:
  def selectGarmentsBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int = ClothingRepository.defaultChunkSize,
  ): Stream[IO, Garment]

object ClothingRepository:
  private val defaultChunkSize = doobie.util.query.DefaultChunkSize

  final class Postgres(transactor: HikariTransactor[IO]) extends ClothingRepository:
    override def selectGarmentsBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int = defaultChunkSize,
    ): Stream[IO, Garment] =
      val connection = GarmentConnection.selectGarmentsBy(filter, sort, chunkSize)
      connection.transact(transactor).map(_.unRow)
