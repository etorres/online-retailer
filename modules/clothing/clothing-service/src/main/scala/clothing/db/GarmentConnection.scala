package es.eriktorr
package clothing.db

import clothing.{Category, Color, Garment as DomainGarment, Size}
import commons.market.EuroMoneyContext.given
import commons.query.Column.*
import commons.query.QueryBuilder.{columns as columnsFr, orderBy, where}
import commons.query.{Column, Filter, Sort}

import doobie.implicits.given
import doobie.postgres.implicits.given
import doobie.util.fragments.parentheses
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import squants.Money

sealed private trait GarmentConnection:
  // Column definitions
  private val idColumn = column[DomainGarment.Id]("id")
  private val categoryColumn = filterable[Category]("category")
  private val modelColumn = filterable[DomainGarment.Model]("model")
  private val sizeColumn = filterable[Size]("size")
  private val colorColumn = filterable[Color]("color")
  private val priceColumn = filterableAndSortable[Money]("price_in_eur")
  private val descriptionColumn = filterable[DomainGarment.Description]("description")
  private val imagesColumn = column[List[String]]("images")

  protected val allColumns: List[Column[?]] = List(
    idColumn,
    categoryColumn,
    modelColumn,
    sizeColumn,
    colorColumn,
    priceColumn,
    descriptionColumn,
    imagesColumn,
  )

  // Filterable columns
  given Filterable[Category] = categoryColumn
  given Filterable[DomainGarment.Model] = modelColumn
  given Filterable[Size] = sizeColumn
  given Filterable[Color] = colorColumn
  given Filterable[Money] = priceColumn

  // Sortable columns
  given sortablePrice: Sortable[Money] = priceColumn

  // Doobie mappers
  given Meta[DomainGarment.Id] = Meta[Long].tiemap(DomainGarment.Id.either)(_.value)
  given Meta[Category] = Meta[String].timap(Category.valueOf)(_.toString)
  given Meta[DomainGarment.Model] = Meta[String].tiemap(DomainGarment.Model.either)(_.value)
  given Meta[Size] = Meta[String].timap(Size.valueOf)(_.toString)
  given Meta[Color] = Meta[String].timap(Color.valueOf)(_.toString)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[DomainGarment.Description] =
    Meta[String].tiemap(DomainGarment.Description.either)(_.value)

object GarmentConnection extends GarmentConnection:
  private val table = fr"garments"

  private val columns = columnsFr(allColumns)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def selectGarmentsBy(filter: Filter, sort: Sort, chunkSize: Int): Stream[ConnectionIO, Garment] =
    val select = fr"SELECT" ++ columns ++ fr"FROM" ++ table
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[Garment].streamWithChunkSize(chunkSize)

  def insert(garment: Garment): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ table ++ parentheses(columns) ++ fr"VALUES" ++ parentheses(
      sql"""${garment.id},
           |${garment.category},
           |${garment.model},
           |${garment.size},
           |${garment.color},
           |${garment.priceInEur},
           |${garment.description},
           |${garment.images}""".stripMargin,
    )
    sql.update.run
