package es.eriktorr
package clothing.db

import clothing.{Category, Color, Garment as DomainGarment, Size}
import commons.domain.{SalesTax, SalesTaxTable}
import commons.market.EuroMoneyContext.given
import commons.query.Column.*
import commons.query.Filter.Comparator.Equal
import commons.query.QueryBuilder.{comma, join, orderBy, where}
import commons.query.{Column, Filter, Sort, Table}

import com.softwaremill.tagging.*
import doobie.implicits.given
import doobie.postgres.implicits.given
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import squants.Money

import java.time.LocalDate

object GarmentTable extends Table[GarmentRow]:
  implicit override val name: Table.Name = "garments".taggedWith[Table.TableNameTag]

  // Column definitions
  private val idColumn = filterable[DomainGarment.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[DomainGarment.Model]("model")
  private val sizeColumn = filterable[Size]("size")
  private val colorColumn = filterable[Color]("color")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val rateColumn = column[DomainGarment.Tax]("rate")(using SalesTaxTable.name)
  private val taxColumn = column[SalesTax]("tax")
  private val descriptionColumn = filterable[DomainGarment.Description]("description")
  private val launchDateColumn = filterableAndSortable[LocalDate]("launch_date")
  private val imagesColumn = column[List[String]]("images")

  override def read: List[Column[_]] = List(
    idColumn,
    categoryColumn,
    modelColumn,
    sizeColumn,
    colorColumn,
    priceColumn,
    rateColumn,
    descriptionColumn,
    launchDateColumn,
    imagesColumn,
  )

  override def write(garmentRow: GarmentRow): Table.Write = Table.Write(
    List(
      idColumn,
      categoryColumn,
      modelColumn,
      sizeColumn,
      colorColumn,
      priceColumn,
      taxColumn,
      descriptionColumn,
      launchDateColumn,
      imagesColumn,
    ),
    sql"""${garmentRow.id},
         |${garmentRow.category},
         |${garmentRow.model},
         |${garmentRow.size},
         |${garmentRow.color},
         |${garmentRow.priceInEur},
         |${garmentRow.tax},
         |${garmentRow.description},
         |${garmentRow.launchDate},
         |${garmentRow.images}""".stripMargin,
  )

  // Filterable columns
  given Filterable[DomainGarment.Id] = idColumn
  given Filterable[Category] = categoryColumn
  given Filterable[DomainGarment.Model] = modelColumn
  given Filterable[Size] = sizeColumn
  given Filterable[Color] = colorColumn
  given Filterable[Money] = priceColumn
  given Filterable[LocalDate] = launchDateColumn

  // Sortable columns
  given sortablePrice: Sortable[Money] = priceColumn
  given sortableLaunchDate: Sortable[LocalDate] = launchDateColumn

  // Doobie mappers
  given Meta[DomainGarment.Id] = Meta[Long].tiemap(DomainGarment.Id.either)(_.value)
  given Meta[Category] = Meta[String].timap(Category.valueOf)(_.toString)
  given Meta[DomainGarment.Model] = Meta[String].tiemap(DomainGarment.Model.either)(_.value)
  given Meta[Size] = Meta[String].timap(Size.valueOf)(_.toString)
  given Meta[Color] = Meta[String].timap(Color.valueOf)(_.toString)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[DomainGarment.Tax] = Meta[Double].tiemap(DomainGarment.Tax.either)(_.value)
  given Meta[DomainGarment.Description] =
    Meta[String].tiemap(DomainGarment.Description.either)(_.value)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def findGarmentBy(id: DomainGarment.Id): ConnectionIO[Option[Garment]] =
    given ColumnFormatter = ColumnFormatter.FullQualifiedName
    val select = fr"SELECT" ++ comma(read) ++ join(
      GarmentTable.taxColumn,
      SalesTaxTable.taxColumn,
    )
    val sql = select ++ where(Equal(id))
    sql.query[Garment].option

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def selectGarmentsBy(filter: Filter, sort: Sort, chunkSize: Int): Stream[ConnectionIO, Garment] =
    given ColumnFormatter = ColumnFormatter.FullQualifiedName
    val select = fr"SELECT" ++ comma(read) ++ join(
      GarmentTable.taxColumn,
      SalesTaxTable.taxColumn,
    )
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[Garment].streamWithChunkSize(chunkSize)

  def insert(garmentRow: GarmentRow): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ this.sql ++ write(garmentRow).sql
    sql.update.run
