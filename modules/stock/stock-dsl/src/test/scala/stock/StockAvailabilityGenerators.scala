package es.eriktorr
package stock

import commons.market.EuroMoneyContext
import commons.market.EuroMoneyContext.given
import commons.spec.StringGenerators.alphaNumericStringBetween

import es.eriktorr.stock.StockAvailability.{Quantity, ReorderLevel}
import org.scalacheck.Gen
import squants.market.Money

object StockAvailabilityGenerators:
  val skuGen: Gen[StockAvailability.SKU] =
    Gen.choose(1L, Long.MaxValue).map(StockAvailability.SKU.applyUnsafe)

  val nameGen: Gen[StockAvailability.Name] =
    alphaNumericStringBetween(3, 5).map(StockAvailability.Name.applyUnsafe)

  val categoryGen: Gen[StockAvailability.Category] =
    alphaNumericStringBetween(5, 7).map(StockAvailability.Category.applyUnsafe)

  val quantityGen: Gen[Quantity] =
    Gen.chooseNum(0, 1000, Seq(0)*).map(StockAvailability.Quantity.applyUnsafe)

  val unitPriceGen: Gen[Money] = (for
    amount <- Gen.choose(1d, 9_000d)
    currency <- Gen.oneOf(euroContext.currencies)
  yield Money(amount, currency))
    .retryUntil(_.in(euroContext.defaultCurrency) <= EuroMoneyContext.max)

  val reorderLevelGen: Gen[ReorderLevel] =
    Gen.choose(100, 1000).map(StockAvailability.ReorderLevel.applyUnsafe)

  def stockAvailabilityGen(
      skuGen: Gen[StockAvailability.SKU] = skuGen,
      nameGen: Gen[StockAvailability.Name] = nameGen,
      categoryGen: Gen[StockAvailability.Category] = categoryGen,
      quantityGen: Gen[Quantity] = quantityGen,
      unitPriceGen: Gen[Money] = unitPriceGen,
      reorderLevelGen: Gen[ReorderLevel] = reorderLevelGen,
  ): Gen[StockAvailability] = for
    sku <- skuGen
    name <- nameGen
    category <- categoryGen
    quantity <- quantityGen
    unitPrice <- unitPriceGen
    reorderLevel <- reorderLevelGen
  yield StockAvailability(sku, name, category, quantity, unitPrice, reorderLevel)
