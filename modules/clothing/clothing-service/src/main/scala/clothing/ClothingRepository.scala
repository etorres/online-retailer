package es.eriktorr
package clothing

import clothing.db.Garment.given
import clothing.db.GarmentTable
import clothing.db.GarmentTable.given
import commons.query.Row.unRow
import commons.query.{Filter, Sort}

import cats.data.OptionT
import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given
import doobie.postgres.implicits.given
import fs2.Stream

trait ClothingRepository:
  def findGarmentBy(id: Garment.Id): OptionT[IO, Garment]
  def selectGarmentsBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int = ClothingRepository.defaultChunkSize,
  ): Stream[IO, Garment]

object ClothingRepository:
  private val defaultChunkSize = doobie.util.query.DefaultChunkSize

  final class Postgres(transactor: HikariTransactor[IO]) extends ClothingRepository:
    override def findGarmentBy(id: Garment.Id): OptionT[IO, Garment] =
      val connection = GarmentTable.findBy(id)
      OptionT(connection.transact(transactor).map(_.unRow))

    override def selectGarmentsBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int = defaultChunkSize,
    ): Stream[IO, Garment] =
      val connection = GarmentTable.selectBy(filter, sort, chunkSize)
      connection.transact(transactor).map(_.unRow)
