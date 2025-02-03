package es.eriktorr
package commons.domain

import commons.query.Column.{column, filterable, Filterable}
import commons.query.{Column, Table}

import com.softwaremill.tagging.*
import doobie.implicits.given
import doobie.Meta

object SalesTaxTable extends Table[SalesTaxRow.Id, SalesTaxRow, Unit]:
  implicit override val name: Table.Name = "taxes".taggedWith[Table.TableNameTag]

  // Column definitions
  val idColumn: Filterable[SalesTaxRow.Id] = filterable[SalesTaxRow.Id]("id")
  val taxColumn: Column[SalesTax] = column[SalesTax]("tax")
  val rateColumn: Column[SalesTaxRow.Rate] = column[SalesTaxRow.Rate]("rate")

  private val allColumns = List(idColumn, taxColumn, rateColumn)

  override def read: List[Column[_]] = allColumns

  override def write(salesTaxRow: SalesTaxRow): Table.Write = Table.Write(
    allColumns,
    sql"""${salesTaxRow.id},
         |${salesTaxRow.tax},
         |${salesTaxRow.rate}""".stripMargin,
  )

  // Filterable columns
  given Filterable[SalesTaxRow.Id] = idColumn

  // Doobie mappers
  given Meta[SalesTaxRow.Id] = Meta[Int].tiemap(SalesTaxRow.Id.either)(_.value)
  given Meta[SalesTaxRow.Rate] = Meta[Double].tiemap(SalesTaxRow.Rate.either)(_.value)
