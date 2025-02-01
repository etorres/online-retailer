package es.eriktorr
package electronics.db

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class TestElectronicsRepository(transactor: HikariTransactor[IO]):
  def add(electronicDeviceRow: ElectronicDeviceRow): IO[Unit] =
    val connection = ElectronicDeviceTable.insert(electronicDeviceRow)
    connection.transact(transactor).void
