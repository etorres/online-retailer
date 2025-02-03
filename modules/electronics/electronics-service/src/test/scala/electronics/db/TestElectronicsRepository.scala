package es.eriktorr
package electronics.db

import electronics.db.ElectronicDeviceTable.ElectronicDeviceDbIn

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestElectronicsRepository(transactor: HikariTransactor[IO]):
  def add(electronicDeviceDbIn: ElectronicDeviceDbIn): IO[Unit] =
    val connection = ElectronicDeviceTable.Impl.insert(electronicDeviceDbIn)
    connection.transact(transactor).void
