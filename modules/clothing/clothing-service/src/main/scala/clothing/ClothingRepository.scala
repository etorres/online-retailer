package es.eriktorr
package clothing

import clothing.db.Garment.given
import clothing.db.GarmentConnection
import commons.query.Row.unRow
import commons.query.{Filter, Sort}

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given

trait ClothingRepository:
  def findGarmentsBy(filter: Filter, sort: Sort): IO[List[Garment]]

object ClothingRepository:
  final class Postgres(transactor: HikariTransactor[IO]) extends ClothingRepository:
    override def findGarmentsBy(filter: Filter, sort: Sort): IO[List[Garment]] =
      val connection = GarmentConnection.findGarmentsBy(filter, sort)
      connection.transact(transactor).map(_.unRow)
