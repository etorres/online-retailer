package es.eriktorr
package stock.db

import commons.market.EuroMoneyContext.given
import commons.query.Column.{column, filterable, filterableAndSortable, Filterable, Sortable}
import commons.query.{Column, Row, Table}
import stock.StockAvailability

import com.softwaremill.tagging.*
import doobie.Meta
import doobie.implicits.given
import io.github.arainko.ducktape.*
import squants.Money

object StockAvailabilityTable:
  final case class StockAvailabilityRow(
      sku: StockAvailability.SKU,
      name: StockAvailability.Name,
      category: StockAvailability.Category,
      quantity: StockAvailability.Quantity,
      unitPriceInEur: Money,
      reorderLevel: StockAvailability.ReorderLevel,
  )

  object StockAvailabilityRow:
    given Row[StockAvailability, StockAvailabilityRow] =
      new Row[StockAvailability, StockAvailabilityRow]:
        override def row(value: StockAvailability): StockAvailabilityRow =
          value
            .into[StockAvailabilityRow]
            .transform(
              Field.computed(_.unitPriceInEur, _.unitPrice.in(euroContext.defaultCurrency)),
            )

        override def unRow(row: StockAvailabilityRow): StockAvailability =
          row
            .into[StockAvailability]
            .transform(
              Field.computed(_.unitPrice, _.unitPriceInEur),
            )

  implicit private val tableName: Table.Name = "stock_availability".taggedWith[Table.TableNameTag]

  // Column definitions
  private val skuColumn = filterable[StockAvailability.SKU]("sku")
  private val nameColumn = column[StockAvailability.Name]("name")
  private val categoryColumn = filterableAndSortable[StockAvailability.Category]("category")
  private val quantityColumn = filterableAndSortable[StockAvailability.Quantity]("quantity")
  private val unitPriceColumn = column[StockAvailability.Quantity]("unit_price_in_eur")
  private val reorderLevelColumn = column[StockAvailability.ReorderLevel]("reorder_level")

  private val allColumns = List(
    skuColumn,
    nameColumn,
    categoryColumn,
    quantityColumn,
    unitPriceColumn,
    reorderLevelColumn,
  )

  // Filterable columns
  given Filterable[StockAvailability.SKU] = skuColumn
  given Filterable[StockAvailability.Category] = categoryColumn
  given Filterable[StockAvailability.Quantity] = quantityColumn

  // Sortable columns
  given sortableCategory: Sortable[StockAvailability.Category] = categoryColumn
  given sortableQuantity: Sortable[StockAvailability.Quantity] = quantityColumn

  // Doobie mappers
  given Meta[StockAvailability.SKU] = Meta[Long].tiemap(StockAvailability.SKU.either)(_.value)
  given Meta[StockAvailability.Name] = Meta[String].tiemap(StockAvailability.Name.either)(_.value)
  given Meta[StockAvailability.Category] =
    Meta[String].tiemap(StockAvailability.Category.either)(_.value)
  given Meta[StockAvailability.Quantity] =
    Meta[Int].tiemap(StockAvailability.Quantity.either)(_.value)
  given Meta[Money] = Meta[BigDecimal].timap(euroContext.defaultCurrency.apply)(
    _.in(euroContext.defaultCurrency).amount,
  )
  given Meta[StockAvailability.ReorderLevel] =
    Meta[Int].tiemap(StockAvailability.ReorderLevel.either)(_.value)

  object Impl extends Table[StockAvailability.SKU, StockAvailabilityRow, StockAvailabilityRow]:
    implicit override val name: Table.Name = tableName

    override def read: List[Column[_]] = allColumns

    override def write(row: StockAvailabilityRow): Table.Write = Table.Write(
      allColumns,
      sql"""${row.sku},
           |${row.name},
           |${row.category},
           |${row.quantity},
           |${row.unitPriceInEur},
           |${row.reorderLevel}""".stripMargin,
    )
