package es.eriktorr
package taxes.db

import taxes.db.TaxesTable.TaxRow

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given

final class TestTaxesRepository(transactor: HikariTransactor[IO]):
  def add(row: TaxRow): IO[Unit] =
    val connection = TaxesTable.Impl.insert(row)
    connection.transact(transactor).void
