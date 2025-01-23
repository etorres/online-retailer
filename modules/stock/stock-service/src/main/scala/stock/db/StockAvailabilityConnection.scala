package es.eriktorr
package stock.db

import commons.market.EuroMoneyContext.given
import commons.query.Column.{column, filterableAndSortable, Filterable, Sortable}
import commons.query.QueryBuilder.{columns as columnsFr, orderBy, where}
import commons.query.{Column, Filter, Sort}
import stock.StockAvailability as DomainStockAvailability

import doobie.implicits.*
import doobie.util.fragments.parentheses
import doobie.{ConnectionIO, Meta}
import fs2.Stream
import squants.Money

sealed private trait StockAvailabilityConnection:
  // Column definitions
  private val skuColumn = column[DomainStockAvailability.SKU]("sku")
  private val nameColumn = column[DomainStockAvailability.Name]("name")
  private val categoryColumn = filterableAndSortable[DomainStockAvailability.Category]("category")
  private val quantityColumn = filterableAndSortable[DomainStockAvailability.Quantity]("quantity")
  private val unitPriceColumn = column[DomainStockAvailability.Quantity]("unit_price_in_eur")
  private val reorderLevelColumn = column[DomainStockAvailability.ReorderLevel]("reorder_level")

  protected val allColumns: List[Column[?]] = List(
    skuColumn,
    nameColumn,
    categoryColumn,
    quantityColumn,
    unitPriceColumn,
    reorderLevelColumn,
  )

  // Filterable columns
  given Filterable[DomainStockAvailability.Category] = categoryColumn
  given Filterable[DomainStockAvailability.Quantity] = quantityColumn

  // Sortable columns
  given sortableCategory: Sortable[DomainStockAvailability.Category] = categoryColumn
  given sortableQuantity: Sortable[DomainStockAvailability.Quantity] = quantityColumn

  // Doobie mappers
  given Meta[DomainStockAvailability.SKU] =
    Meta[Long].tiemap(DomainStockAvailability.SKU.either)(_.value)
  given Meta[DomainStockAvailability.Name] =
    Meta[String].tiemap(DomainStockAvailability.Name.either)(_.value)
  given Meta[DomainStockAvailability.Category] =
    Meta[String].tiemap(DomainStockAvailability.Category.either)(_.value)
  given Meta[DomainStockAvailability.Quantity] =
    Meta[Int].tiemap(DomainStockAvailability.Quantity.either)(_.value)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[DomainStockAvailability.ReorderLevel] =
    Meta[Int].tiemap(DomainStockAvailability.ReorderLevel.either)(_.value)

object StockAvailabilityConnection extends StockAvailabilityConnection:
  private val table = fr"stock_availability"

  private val columns = columnsFr(allColumns)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def selectStockAvailabilitiesBy(
      filter: Filter,
      sort: Sort,
      chunkSize: Int,
  ): Stream[ConnectionIO, StockAvailability] =
    val select = fr"SELECT" ++ columns ++ fr"FROM" ++ table
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[StockAvailability].streamWithChunkSize(chunkSize)

  def insert(stockAvailability: StockAvailability): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ table ++ parentheses(columns) ++ fr"VALUES" ++ parentheses(
      sql"""${stockAvailability.sku},
           |${stockAvailability.name},
           |${stockAvailability.category},
           |${stockAvailability.quantity},
           |${stockAvailability.unitPriceInEur},
           |${stockAvailability.reorderLevel}""".stripMargin,
    )
    sql.update.run
