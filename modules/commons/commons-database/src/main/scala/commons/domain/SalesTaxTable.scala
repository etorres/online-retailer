package es.eriktorr
package commons.domain

import commons.query.Column.column
import commons.query.{Column, Table}

import com.softwaremill.tagging.*
import doobie.free.connection.ConnectionIO
import doobie.implicits.*
import doobie.Meta

object SalesTaxTable extends Table[SalesTaxRow]:
  implicit override val name: Table.Name = "taxes".taggedWith[Table.TableNameTag]

  val idColumn: Column[SalesTaxRow.Id] = column[SalesTaxRow.Id]("id")
  val taxColumn: Column[SalesTax] = column[SalesTax]("tax")
  val rateColumn: Column[SalesTaxRow.Rate] = column[SalesTaxRow.Rate]("rate")

  private val allColumns: List[Column[_]] = List(idColumn, taxColumn, rateColumn)

  override def read: List[Column[_]] = allColumns

  override def write(salesTaxRow: SalesTaxRow): Table.Write = Table.Write(
    allColumns,
    sql"""${salesTaxRow.id},
         |${salesTaxRow.tax},
         |${salesTaxRow.rate}""".stripMargin,
  )

  given Meta[SalesTaxRow.Id] = Meta[Int].tiemap(SalesTaxRow.Id.either)(_.value)
  given Meta[SalesTaxRow.Rate] = Meta[Double].tiemap(SalesTaxRow.Rate.either)(_.value)

  def insert(salesTaxRow: SalesTaxRow): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ this.sql ++ write(salesTaxRow).sql
    sql.update.run
