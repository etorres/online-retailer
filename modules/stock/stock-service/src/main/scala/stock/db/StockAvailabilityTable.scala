package es.eriktorr
package stock.db

import commons.market.EuroMoneyContext.given
import commons.query.Column.{column, filterableAndSortable, Filterable, Sortable}
import commons.query.{Column, Table}
import stock.StockAvailability as DomainStockAvailability

import com.softwaremill.tagging.*
import doobie.Meta
import doobie.implicits.*
import squants.Money

object StockAvailabilityTable
    extends Table[DomainStockAvailability.SKU, StockAvailability, StockAvailability]:
  implicit override val name: Table.Name = "stock_availability".taggedWith[Table.TableNameTag]

  // Column definitions
  private val skuColumn = column[DomainStockAvailability.SKU]("sku")
  private val nameColumn = column[DomainStockAvailability.Name]("name")
  private val categoryColumn = filterableAndSortable[DomainStockAvailability.Category]("category")
  private val quantityColumn = filterableAndSortable[DomainStockAvailability.Quantity]("quantity")
  private val unitPriceColumn = column[DomainStockAvailability.Quantity]("unit_price_in_eur")
  private val reorderLevelColumn = column[DomainStockAvailability.ReorderLevel]("reorder_level")

  private val allColumns = List(
    skuColumn,
    nameColumn,
    categoryColumn,
    quantityColumn,
    unitPriceColumn,
    reorderLevelColumn,
  )

  override def read: List[Column[_]] = allColumns

  override def write(stockAvailability: StockAvailability): Table.Write = Table.Write(
    allColumns,
    sql"""${stockAvailability.sku},
         |${stockAvailability.name},
         |${stockAvailability.category},
         |${stockAvailability.quantity},
         |${stockAvailability.unitPriceInEur},
         |${stockAvailability.reorderLevel}""".stripMargin,
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
