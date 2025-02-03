package es.eriktorr
package clothing.db

import clothing.db.GarmentTable.GarmentDbIn

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestClothingRepository(transactor: HikariTransactor[IO]):
  def add(garmentDbIn: GarmentDbIn): IO[Unit] =
    val connection = GarmentTable.Impl.insert(garmentDbIn)
    connection.transact(transactor).void
