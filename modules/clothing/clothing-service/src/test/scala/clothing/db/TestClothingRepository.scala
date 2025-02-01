package es.eriktorr
package clothing.db

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestClothingRepository(transactor: HikariTransactor[IO]):
  def add(garmentRow: GarmentRow): IO[Unit] =
    val connection = GarmentTable.insert(garmentRow)
    connection.transact(transactor).void
