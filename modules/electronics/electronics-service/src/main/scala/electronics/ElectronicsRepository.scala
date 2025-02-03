package es.eriktorr
package electronics

import commons.query.Row.unRow
import commons.query.{Filter, Sort}
import electronics.db.ElectronicDeviceTable
import electronics.db.ElectronicDeviceTable.given

import cats.data.OptionT
import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.given
import doobie.postgres.implicits.given
import fs2.Stream

trait ElectronicsRepository:
  def findElectronicDeviceBy(id: ElectronicDevice.Id): OptionT[IO, ElectronicDevice]
  def selectElectronicDevicesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int = ElectronicsRepository.defaultChunkSize,
  ): Stream[IO, ElectronicDevice]

object ElectronicsRepository:
  private val defaultChunkSize = doobie.util.query.DefaultChunkSize

  final class Postgres(transactor: HikariTransactor[IO]) extends ElectronicsRepository:
    override def findElectronicDeviceBy(id: ElectronicDevice.Id): OptionT[IO, ElectronicDevice] =
      val connection = ElectronicDeviceTable.findBy(id)
      OptionT(connection.transact(transactor).map(_.unRow))

    override def selectElectronicDevicesBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int = defaultChunkSize,
    ): Stream[IO, ElectronicDevice] =
      val connection = ElectronicDeviceTable.selectBy(filter, sort, chunkSize)
      connection.transact(transactor).map(_.unRow)
