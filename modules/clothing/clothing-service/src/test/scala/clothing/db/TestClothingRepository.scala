package es.eriktorr
package clothing.db

import clothing.db.Garment.given
import clothing.Garment
import commons.query.Row.row

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestClothingRepository(transactor: HikariTransactor[IO]):
  def add(garment: Garment): IO[Unit] =
    val connection = GarmentConnection.insert(garment.row)
    connection.transact(transactor).void
