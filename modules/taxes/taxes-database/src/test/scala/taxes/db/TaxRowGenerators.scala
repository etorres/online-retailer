package es.eriktorr
package taxes.db

import taxes.{SalesTax, Tax}
import taxes.TaxGenerators.{idGen, rateGen, salesTaxGen, taxGen}
import taxes.db.TaxesTable.TaxRow

import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

object TaxRowGenerators:
  def taxRowGen(
      idGen: Gen[Tax.Id] = idGen,
      salesTaxGen: Gen[SalesTax] = salesTaxGen,
      rateGen: Gen[Tax.Rate] = rateGen,
  ): Gen[TaxRow] = taxGen(idGen, salesTaxGen, rateGen).map: tax =>
    TaxRow(tax.id, tax.tax, tax.rate)

  val taxRowsGen: Gen[List[TaxRow]] =
    SalesTax.values.toList.zipWithIndex.traverse:
      case (salesTax, idx) =>
        taxRowGen(idGen = Tax.Id.applyUnsafe(idx), salesTaxGen = salesTax)
