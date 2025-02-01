package es.eriktorr
package commons.domain

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestSalesTaxRepository(transactor: HikariTransactor[IO]):
  def add(salesTaxRow: SalesTaxRow): IO[Unit] =
    val connection = SalesTaxTable.insert(salesTaxRow)
    connection.transact(transactor).void
