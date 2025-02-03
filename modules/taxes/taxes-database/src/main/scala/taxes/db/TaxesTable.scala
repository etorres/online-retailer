package es.eriktorr
package taxes.db

import commons.query.{Column, Row, Table}
import commons.query.Column.{column, filterable, Filterable}
import taxes.{SalesTax, Tax}

import com.softwaremill.tagging.*
import doobie.Meta
import doobie.implicits.given
import doobie.postgres.implicits.pgEnumStringOpt
import io.github.arainko.ducktape.*

object TaxesTable:
  final case class TaxRow(id: Tax.Id, tax: SalesTax, rate: Tax.Rate)

  object TaxRow:
    given Row[Tax, TaxRow] = new Row[Tax, TaxRow]:
      override def row(tax: Tax): TaxRow = tax.to[TaxRow]
      override def unRow(row: TaxRow): Tax = row.to[Tax]

  implicit private val tableName: Table.Name = "taxes".taggedWith[Table.TableNameTag]

  // Column definitions
  private val idColumn = filterable[Tax.Id]("id")
  val taxColumn: Column[SalesTax] = column[SalesTax]("tax")
  private val rateColumn = column[Tax.Rate]("rate")

  private val allColumns = List(idColumn, taxColumn, rateColumn)

  // Filterable columns
  given Filterable[Tax.Id] = idColumn

  // Doobie mappers
  given Meta[Tax.Id] = Meta[Int].tiemap(Tax.Id.either)(_.value)
  given Meta[SalesTax] = pgEnumStringOpt("sales_tax", SalesTax.option, _.value)
  given Meta[Tax.Rate] = Meta[Double].tiemap(Tax.Rate.either)(_.value)

  object Impl extends Table[Tax.Id, TaxRow, TaxRow]:
    implicit override val name: Table.Name = tableName

    override def read: List[Column[_]] = allColumns

    override def write(dbIn: TaxRow): Table.Write = Table.Write(
      allColumns,
      sql"""${dbIn.id},
           |${dbIn.tax},
           |${dbIn.rate}""".stripMargin,
    )
