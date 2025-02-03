package es.eriktorr
package electronics.db

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
import commons.query.*
import electronics.{Category, ElectronicDevice}
import taxes.{SalesTax, Tax}
import taxes.db.TaxesTable
import taxes.db.TaxesTable.given

import com.softwaremill.tagging.*
import doobie.implicits.given
import doobie.postgres.implicits.given
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import io.github.arainko.ducktape.*
import squants.energy.Watts
import squants.{Money, Power}

import java.time.LocalDate

object ElectronicDeviceTable:
  final case class ElectronicDeviceDbIn(
      id: ElectronicDevice.Id,
      category: Category,
      model: ElectronicDevice.Model,
      powerConsumptionInWatts: Power,
      priceInEur: Money,
      tax: SalesTax,
      description: ElectronicDevice.Description,
      launchDate: LocalDate,
      images: List[String],
  )

  object ElectronicDeviceDbIn:
    def unRowWith(
        deviceDbIn: ElectronicDeviceDbIn,
        salesTaxToRate: Map[SalesTax, Tax.Rate],
    ): Option[ElectronicDevice] =
      for
        rate <- salesTaxToRate.get(deviceDbIn.tax)
        tax <- ElectronicDevice.Tax.option(rate)
        electronicDevice = deviceDbIn
          .into[ElectronicDevice]
          .transform(
            Field.computed(_.powerConsumption, _.powerConsumptionInWatts),
            Field.computed(_.price, _.priceInEur),
            Field.computed(_.tax, _ => ElectronicDevice.Tax.applyUnsafe(rate)),
          )
      yield electronicDevice

  final case class ElectronicDeviceDbOut(
      id: ElectronicDevice.Id,
      category: Category,
      model: ElectronicDevice.Model,
      powerConsumptionInWatts: Power,
      priceInEur: Money,
      tax: ElectronicDevice.Tax,
      description: ElectronicDevice.Description,
      launchDate: LocalDate,
      images: List[String],
  )

  object ElectronicDeviceDbOut:
    given Row[ElectronicDevice, ElectronicDeviceDbOut] =
      new Row[ElectronicDevice, ElectronicDeviceDbOut]:
        override def row(electronicDevice: ElectronicDevice): ElectronicDeviceDbOut =
          electronicDevice
            .into[ElectronicDeviceDbOut]
            .transform(
              Field.computed(_.powerConsumptionInWatts, _.powerConsumption.in(Watts)),
              Field.computed(_.priceInEur, _.price.in(euroContext.defaultCurrency)),
            )

        override def unRow(electronicDeviceDbOut: ElectronicDeviceDbOut): ElectronicDevice =
          electronicDeviceDbOut
            .into[ElectronicDevice]
            .transform(
              Field.computed(_.powerConsumption, _.powerConsumptionInWatts),
              Field.computed(_.price, _.priceInEur),
            )

  implicit private val tableName: Table.Name = "electronics".taggedWith[Table.TableNameTag]

  // Column definitions
  private val idColumn = filterable[ElectronicDevice.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[ElectronicDevice.Model]("model")
  private val powerConsumptionColumn = filterableAndSortable[Power]("power_consumption_in_watts")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val rateColumn = column[ElectronicDevice.Tax]("rate")(using TaxesTable.Impl.name)
  private val taxColumn = column[SalesTax]("tax")
  private val descriptionColumn = filterable[ElectronicDevice.Description]("description")
  private val launchDateColumn = filterableAndSortable[LocalDate]("launch_date")
  private val imagesColumn = column[List[String]]("images")

  // Filterable columns
  given Filterable[ElectronicDevice.Id] = idColumn
  given Filterable[Category] = categoryColumn
  given Filterable[ElectronicDevice.Model] = modelColumn
  given Filterable[Power] = powerConsumptionColumn
  given Filterable[Money] = priceColumn
  given Filterable[LocalDate] = launchDateColumn

  // Sortable columns
  given sortablePower: Sortable[Power] = powerConsumptionColumn
  given sortablePrice: Sortable[Money] = priceColumn
  given sortableLaunchDate: Sortable[LocalDate] = launchDateColumn

  // Doobie mappers
  given Meta[ElectronicDevice.Id] = Meta[Long].tiemap(ElectronicDevice.Id.either)(_.value)
  given Meta[Category] = Meta[String].timap(Category.valueOf)(_.toString)
  given Meta[ElectronicDevice.Model] = Meta[String].tiemap(ElectronicDevice.Model.either)(_.value)
  given Meta[Power] = Meta[BigDecimal].timap(Watts.apply)(_.in(Watts).value)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[ElectronicDevice.Tax] = Meta[Double].tiemap(ElectronicDevice.Tax.either)(_.value)
  given Meta[ElectronicDevice.Description] =
    Meta[String].tiemap(ElectronicDevice.Description.either)(_.value)

  object Impl extends Table[ElectronicDevice.Id, ElectronicDeviceDbIn, ElectronicDeviceDbOut]:
    implicit override val name: Table.Name = tableName

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

    override def write(electronicDeviceDbIn: ElectronicDeviceDbIn): Table.Write = Table.Write(
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
      sql"""${electronicDeviceDbIn.id},
           |${electronicDeviceDbIn.category},
           |${electronicDeviceDbIn.model},
           |${electronicDeviceDbIn.powerConsumptionInWatts},
           |${electronicDeviceDbIn.priceInEur},
           |${electronicDeviceDbIn.tax},
           |${electronicDeviceDbIn.description},
           |${electronicDeviceDbIn.launchDate},
           |${electronicDeviceDbIn.images}""".stripMargin,
    )

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    override def findBy(id: ElectronicDevice.Id): ConnectionIO[Option[ElectronicDeviceDbOut]] =
      given ColumnFormatter = ColumnFormatter.FullQualifiedName
      val select = fr"SELECT" ++ comma(read) ++ join(
        ElectronicDeviceTable.taxColumn,
        TaxesTable.taxColumn,
      )
      val sql = select ++ where(Equal(id))
      sql.query[ElectronicDeviceDbOut].option

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    override def selectBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int,
    ): Stream[ConnectionIO, ElectronicDeviceDbOut] =
      given ColumnFormatter = ColumnFormatter.FullQualifiedName
      val select = fr"SELECT" ++ comma(read) ++ join(
        ElectronicDeviceTable.taxColumn,
        TaxesTable.taxColumn,
      )
      val sql = select ++ where(filter) ++ orderBy(sort)
      sql.query[ElectronicDeviceDbOut].streamWithChunkSize(chunkSize)
