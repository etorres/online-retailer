package es.eriktorr
package electronics.db

import commons.query.Row.row
import electronics.db.ElectronicDevice.given
import electronics.{db, ElectronicDevice}

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestElectronicsRepository(transactor: HikariTransactor[IO]):
  def add(electronicDevice: ElectronicDevice): IO[Unit] =
    val connection = ElectronicDeviceConnection.insert(electronicDevice.row)
    connection.transact(transactor).void
