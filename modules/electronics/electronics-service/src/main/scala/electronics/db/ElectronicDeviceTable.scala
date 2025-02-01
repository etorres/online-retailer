package es.eriktorr
package electronics.db

import commons.domain.{SalesTax, SalesTaxTable}
import commons.market.EuroMoneyContext.given
import commons.query.Column.{
  column,
  filterable,
  filterableAndSortable,
  ColumnFormatter,
  Filterable,
  Sortable,
}
import commons.query.Filter.Comparator.Equal
import commons.query.QueryBuilder.{comma, join, orderBy, where}
import commons.query.{Column, Filter, QueryBuilder, Sort, Table}
import electronics.{Category, ElectronicDevice as DomainElectronicDevice}

import com.softwaremill.tagging.*
import doobie.implicits.*
import doobie.postgres.implicits.given
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import squants.energy.Watts
import squants.{Money, Power}

import java.time.LocalDate

object ElectronicDeviceTable extends Table[ElectronicDeviceRow]:
  implicit override val name: Table.Name = "electronics".taggedWith[Table.TableNameTag]

  // Column definitions
  private val idColumn = filterable[DomainElectronicDevice.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[DomainElectronicDevice.Model]("model")
  private val powerConsumptionColumn = filterableAndSortable[Power]("power_consumption_in_watts")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val rateColumn = column[DomainElectronicDevice.Tax]("rate")(using SalesTaxTable.name)
  private val taxColumn = column[SalesTax]("tax")
  private val descriptionColumn = filterable[DomainElectronicDevice.Description]("description")
  private val launchDateColumn = filterableAndSortable[LocalDate]("launch_date")
  private val imagesColumn = column[List[String]]("images")

  override def read: List[Column[_]] = List(
    idColumn,
    categoryColumn,
    modelColumn,
    powerConsumptionColumn,
    priceColumn,
    rateColumn,
    descriptionColumn,
    launchDateColumn,
    imagesColumn,
  )

  override def write(electronicDeviceRow: ElectronicDeviceRow): Table.Write = Table.Write(
    List(
      idColumn,
      categoryColumn,
      modelColumn,
      powerConsumptionColumn,
      priceColumn,
      taxColumn,
      descriptionColumn,
      launchDateColumn,
      imagesColumn,
    ),
    sql"""${electronicDeviceRow.id},
         |${electronicDeviceRow.category},
         |${electronicDeviceRow.model},
         |${electronicDeviceRow.powerConsumptionInWatts},
         |${electronicDeviceRow.priceInEur},
         |${electronicDeviceRow.tax},
         |${electronicDeviceRow.description},
         |${electronicDeviceRow.launchDate},
         |${electronicDeviceRow.images}""".stripMargin,
  )

  // Filterable columns
  given Filterable[DomainElectronicDevice.Id] = idColumn
  given Filterable[Category] = categoryColumn
  given Filterable[DomainElectronicDevice.Model] = modelColumn
  given Filterable[Power] = powerConsumptionColumn
  given Filterable[Money] = priceColumn
  given Filterable[LocalDate] = launchDateColumn

  // Sortable columns
  given sortablePower: Sortable[Power] = powerConsumptionColumn
  given sortablePrice: Sortable[Money] = priceColumn
  given sortableLaunchDate: Sortable[LocalDate] = launchDateColumn

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
  given Meta[DomainElectronicDevice.Tax] =
    Meta[Double].tiemap(DomainElectronicDevice.Tax.either)(_.value)
  given Meta[DomainElectronicDevice.Description] =
    Meta[String].tiemap(DomainElectronicDevice.Description.either)(_.value)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def findElectronicDeviceBy(
      id: DomainElectronicDevice.Id,
  ): ConnectionIO[Option[ElectronicDevice]] =
    given ColumnFormatter = ColumnFormatter.FullQualifiedName
    val select = fr"SELECT" ++ comma(read) ++ join(
      ElectronicDeviceTable.taxColumn,
      SalesTaxTable.taxColumn,
    )
    val sql = select ++ where(Equal(id))
    sql.query[ElectronicDevice].option

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def selectElectronicDevicesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[ConnectionIO, ElectronicDevice] =
    given ColumnFormatter = ColumnFormatter.FullQualifiedName
    val select = fr"SELECT" ++ comma(read) ++ join(
      ElectronicDeviceTable.taxColumn,
      SalesTaxTable.taxColumn,
    )
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[ElectronicDevice].streamWithChunkSize(chunkSize)

  def insert(electronicDeviceRow: ElectronicDeviceRow): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ this.sql ++ write(electronicDeviceRow).sql
    sql.update.run
