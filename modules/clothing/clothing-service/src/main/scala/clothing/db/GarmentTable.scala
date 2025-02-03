package es.eriktorr
package clothing.db

import clothing.{Category, Color, Garment, Size}
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
import taxes.{SalesTax, Tax}
import taxes.db.TaxesTable
import taxes.db.TaxesTable.given

import com.softwaremill.tagging.*
import doobie.implicits.given
import doobie.postgres.implicits.given
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import io.github.arainko.ducktape.*
import squants.Money

import java.time.LocalDate

object GarmentTable:
  final case class GarmentDbIn(
      id: Garment.Id,
      category: Category,
      model: Garment.Model,
      size: Size,
      color: Color,
      priceInEur: Money,
      tax: SalesTax,
      description: Garment.Description,
      launchDate: LocalDate,
      images: List[String],
  )

  object GarmentDbIn:
    def unRowWith(
        garmentRow: GarmentDbIn,
        salesTaxToRate: Map[SalesTax, Tax.Rate],
    ): Option[Garment] =
      for
        rate <- salesTaxToRate.get(garmentRow.tax)
        tax <- Garment.Tax.option(rate)
        garment = garmentRow
          .into[Garment]
          .transform(
            Field.computed(_.price, _.priceInEur),
            Field.computed(_.tax, _ => Garment.Tax.applyUnsafe(rate)),
          )
      yield garment

  final case class GarmentDbOut(
      id: Garment.Id,
      category: Category,
      model: Garment.Model,
      size: Size,
      color: Color,
      priceInEur: Money,
      tax: Garment.Tax,
      description: Garment.Description,
      launchDate: LocalDate,
      images: List[String],
  )

  object GarmentDbOut:
    given Row[Garment, GarmentDbOut] = new Row[Garment, GarmentDbOut]:
      override def row(garment: Garment): GarmentDbOut =
        garment
          .into[GarmentDbOut]
          .transform(
            Field.computed(_.priceInEur, _.price.in(euroContext.defaultCurrency)),
          )

      override def unRow(garmentDbOut: GarmentDbOut): Garment =
        garmentDbOut
          .into[Garment]
          .transform(
            Field.computed(_.price, _.priceInEur),
          )

  implicit private val tableName: Table.Name = "garments".taggedWith[Table.TableNameTag]

  // Column definitions
  private val idColumn = filterable[Garment.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[Garment.Model]("model")
  private val sizeColumn = filterable[Size]("size")
  private val colorColumn = filterable[Color]("color")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val rateColumn = column[Garment.Tax]("rate")(using TaxesTable.Impl.name)
  private val taxColumn = column[SalesTax]("tax")
  private val descriptionColumn = filterable[Garment.Description]("description")
  private val launchDateColumn = filterableAndSortable[LocalDate]("launch_date")
  private val imagesColumn = column[List[String]]("images")

  // Filterable columns
  given Filterable[Garment.Id] = idColumn
  given Filterable[Category] = categoryColumn
  given Filterable[Garment.Model] = modelColumn
  given Filterable[Size] = sizeColumn
  given Filterable[Color] = colorColumn
  given Filterable[Money] = priceColumn
  given Filterable[LocalDate] = launchDateColumn

  // Sortable columns
  given sortablePrice: Sortable[Money] = priceColumn
  given sortableLaunchDate: Sortable[LocalDate] = launchDateColumn

  // Doobie mappers
  given Meta[Garment.Id] = Meta[Long].tiemap(Garment.Id.either)(_.value)
  given Meta[Category] = Meta[String].timap(Category.valueOf)(_.toString)
  given Meta[Garment.Model] = Meta[String].tiemap(Garment.Model.either)(_.value)
  given Meta[Size] = Meta[String].timap(Size.valueOf)(_.toString)
  given Meta[Color] = Meta[String].timap(Color.valueOf)(_.toString)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[Garment.Tax] = Meta[Double].tiemap(Garment.Tax.either)(_.value)
  given Meta[Garment.Description] = Meta[String].tiemap(Garment.Description.either)(_.value)

  object Impl extends Table[Garment.Id, GarmentDbIn, GarmentDbOut]:
    implicit override val name: Table.Name = tableName

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

    override def write(garmentDbIn: GarmentDbIn): Table.Write = Table.Write(
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
      sql"""${garmentDbIn.id},
           |${garmentDbIn.category},
           |${garmentDbIn.model},
           |${garmentDbIn.size},
           |${garmentDbIn.color},
           |${garmentDbIn.priceInEur},
           |${garmentDbIn.tax},
           |${garmentDbIn.description},
           |${garmentDbIn.launchDate},
           |${garmentDbIn.images}""".stripMargin,
    )

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    override def findBy(id: Garment.Id): ConnectionIO[Option[GarmentDbOut]] =
      given ColumnFormatter = ColumnFormatter.FullQualifiedName
      val select = fr"SELECT" ++ comma(read) ++ join(
        taxColumn,
        TaxesTable.taxColumn,
      )
      val sql = select ++ where(Equal(id))
      sql.query[GarmentDbOut].option

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    override def selectBy(
        filter: Filter,
        sort: Sort,
        chunkSize: Int,
    ): Stream[ConnectionIO, GarmentDbOut] =
      given ColumnFormatter = ColumnFormatter.FullQualifiedName
      val select = fr"SELECT" ++ comma(read) ++ join(
        taxColumn,
        TaxesTable.taxColumn,
      )
      val sql = select ++ where(filter) ++ orderBy(sort)
      sql.query[GarmentDbOut].streamWithChunkSize(chunkSize)
