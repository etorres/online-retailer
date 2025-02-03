package es.eriktorr
package taxes

import commons.query.Row.unRow
import commons.query.{Filter, Sort}
import taxes.Tax.Id
import taxes.db.TaxesTable

import cats.data.OptionT
import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given
import fs2.Stream

trait TaxesRepository:
  def findTaxBy(id: Tax.Id): OptionT[IO, Tax]
  def selectTaxesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int = TaxesRepository.defaultChunkSize,
  ): Stream[IO, Tax]

object TaxesRepository:
  private val defaultChunkSize = doobie.util.query.DefaultChunkSize

  final class Postgres(transactor: HikariTransactor[IO]) extends TaxesRepository:
    override def findTaxBy(id: Id): OptionT[IO, Tax] =
      val connection = TaxesTable.Impl.findBy(id)
      OptionT(connection.transact(transactor).map(_.unRow))

    override def selectTaxesBy(filter: Filter, sort: Sort, chunkSize: Int): Stream[IO, Tax] =
      val connection = TaxesTable.Impl.selectBy(filter, sort, chunkSize)
      connection.transact(transactor).map(_.unRow)
