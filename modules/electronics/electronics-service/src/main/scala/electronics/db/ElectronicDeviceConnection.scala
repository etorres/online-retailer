package es.eriktorr
package electronics.db

import commons.market.EuroMoneyContext.given
import commons.query.Column.{column, filterable, filterableAndSortable, Filterable, Sortable}
import commons.query.Filter.Comparator.Equal
import commons.query.QueryBuilder.{columns as columnsFr, orderBy, where}
import commons.query.{Column, Filter, Sort}
import electronics.{Category, ElectronicDevice as DomainElectronicDevice}

import doobie.implicits.*
import doobie.postgres.implicits.given
import doobie.util.fragments.parentheses
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import squants.energy.Watts
import squants.{Money, Power}

sealed private trait ElectronicDeviceConnection:
  // Column definitions
  private val idColumn = filterable[DomainElectronicDevice.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[DomainElectronicDevice.Model]("model")
  private val powerConsumptionColumn = filterableAndSortable[Power]("power_consumption_in_watts")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val descriptionColumn = filterable[DomainElectronicDevice.Description]("description")
  private val imagesColumn = column[List[String]]("images")

  protected val allColumns: List[Column[?]] = List(
    idColumn,
    categoryColumn,
    modelColumn,
    powerConsumptionColumn,
    priceColumn,
    descriptionColumn,
    imagesColumn,
  )

  // Filterable columns
  given Filterable[DomainElectronicDevice.Id] = idColumn
  given Filterable[Category] = categoryColumn
  given Filterable[DomainElectronicDevice.Model] = modelColumn
  given Filterable[Power] = powerConsumptionColumn
  given Filterable[Money] = priceColumn

  // Sortable columns
  given sortablePower: Sortable[Power] = powerConsumptionColumn
  given sortablePrice: Sortable[Money] = priceColumn

  // Doobie mappers
  given Meta[DomainElectronicDevice.Id] =
    Meta[Long].tiemap(DomainElectronicDevice.Id.either)(_.value)
  given Meta[Category] = Meta[String].timap(Category.valueOf)(_.toString)
  given Meta[DomainElectronicDevice.Model] =
    Meta[String].tiemap(DomainElectronicDevice.Model.either)(_.value)
  given Meta[Power] = Meta[BigDecimal].timap(Watts.apply)(_.in(Watts).value)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[DomainElectronicDevice.Description] =
    Meta[String].tiemap(DomainElectronicDevice.Description.either)(_.value)

object ElectronicDeviceConnection extends ElectronicDeviceConnection:
  private val table = fr"electronics"

  private val columns = columnsFr(allColumns)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def findElectronicDeviceBy(
      id: DomainElectronicDevice.Id,
  ): ConnectionIO[Option[ElectronicDevice]] =
    val select = fr"SELECT" ++ columns ++ fr"FROM" ++ table
    val sql = select ++ where(Equal(id))
    sql.query[ElectronicDevice].option

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def selectElectronicDevicesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[ConnectionIO, ElectronicDevice] =
    val select = fr"SELECT" ++ columns ++ fr"FROM" ++ table
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[ElectronicDevice].streamWithChunkSize(chunkSize)

  def insert(electronicDevice: ElectronicDevice): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ table ++ parentheses(columns) ++ fr"VALUES" ++ parentheses(
      sql"""${electronicDevice.id},
           |${electronicDevice.category},
           |${electronicDevice.model},
           |${electronicDevice.powerConsumptionInWatts},
           |${electronicDevice.priceInEur},
           |${electronicDevice.description},
           |${electronicDevice.images}""".stripMargin,
    )
    sql.update.run
