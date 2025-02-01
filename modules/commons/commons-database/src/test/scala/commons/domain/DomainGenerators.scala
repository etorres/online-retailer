package es.eriktorr
package commons.domain

import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given

object DomainGenerators:
  val idGen: Gen[SalesTaxRow.Id] =
    Gen.choose(0, Short.MaxValue.toInt).map(SalesTaxRow.Id.applyUnsafe)

  val salesTaxGen: Gen[SalesTax] = Gen.oneOf(SalesTax.values.toList)

  val rateGen: Gen[SalesTaxRow.Rate] = Gen.choose(0d, 1d).map(SalesTaxRow.Rate.applyUnsafe)

  def salesTaxRowGen(
      idGen: Gen[SalesTaxRow.Id] = idGen,
      taxGen: Gen[SalesTax] = salesTaxGen,
      rateGen: Gen[SalesTaxRow.Rate] = rateGen,
  ): Gen[SalesTaxRow] = for
    id <- idGen
    tax <- taxGen
    rate <- rateGen
  yield SalesTaxRow(id, tax, rate)

  val salesTaxRowsGen: Gen[List[SalesTaxRow]] =
    SalesTax.values.toList.zipWithIndex.traverse:
      case (salesTax, idx) =>
        salesTaxRowGen(idGen = SalesTaxRow.Id.applyUnsafe(idx), taxGen = salesTax)
