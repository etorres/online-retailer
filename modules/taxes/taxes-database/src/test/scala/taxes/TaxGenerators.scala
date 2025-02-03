package es.eriktorr
package taxes

import org.scalacheck.Gen

object TaxGenerators:
  val idGen: Gen[Tax.Id] = Gen.choose(0, Short.MaxValue.toInt).map(Tax.Id.applyUnsafe)

  val salesTaxGen: Gen[SalesTax] = Gen.oneOf(SalesTax.values.toList)

  val rateGen: Gen[Tax.Rate] = Gen.choose(0d, 1d).map(Tax.Rate.applyUnsafe)

  def taxGen(
      idGen: Gen[Tax.Id] = idGen,
      salesTaxGen: Gen[SalesTax] = salesTaxGen,
      rateGen: Gen[Tax.Rate] = rateGen,
  ): Gen[Tax] = for
    id <- idGen
    tax <- salesTaxGen
    rate <- rateGen
  yield Tax(id, tax, rate)
