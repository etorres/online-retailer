package es.eriktorr
package stock

import commons.market.EuroMoneyContext.given
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.{Ascending, Descending, NoSort}
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.PostgresSuite
import commons.spec.RangeGenerators.rangeIntGen
import stock.StockAvailabilityGenerators.{categoryGen, skuGen, stockAvailabilityGen}
import stock.StockRepositorySuite.{filterAndSortTestCaseGen, selectAllTestCaseGen, TestCase}
import stock.db.StockAvailabilityTable.given
import stock.db.TestStockRepository

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF

import scala.util.Random

final class StockRepositorySuite extends PostgresSuite:
  test("should list all stock availabilities"):
    testWith(selectAllTestCaseGen)

  test("should filter and sort stock availabilities"):
    testWith(filterAndSortTestCaseGen)

  private def testWith(testCaseGen: Gen[TestCase]) =
    forAllF(testCaseGen):
      case TestCase(stockAvailabilities, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testStockRepository = TestStockRepository(transactor)
          val testee = StockRepository.Postgres(transactor)
          (for
            _ <- stockAvailabilities.traverse_(testStockRepository.add)
            obtained <- testee.selectStockAvailabilitiesBy(filter, sort).compile.toList
          yield diff(obtained)).assertEquals(diff(expected))

object StockRepositorySuite:
  final private case class TestCase(
      stockAvailabilities: List[StockAvailability],
      filter: Filter,
      sort: Sort,
      expected: List[StockAvailability],
      diff: List[StockAvailability] => List[StockAvailability],
  )

  private val sortBySku = (xs: List[StockAvailability]) => xs.sortBy(_.sku)

  private val selectAllTestCaseGen = for
    size <- Gen.choose(3, 5)
    skus <- nDistinct(size, skuGen)
    stockAvailabilities <- skus.traverse(sku => stockAvailabilityGen(sku))
    expected = stockAvailabilities.map: stockAvailability =>
      val roundedPrice = stockAvailability.unitPrice
        .in(euroContext.defaultCurrency)
        .map(amount => BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue)
      stockAvailability.copy(unitPrice = roundedPrice)
  yield TestCase(stockAvailabilities, NoFilter, NoSort, expected, sortBySku)

  private val filterAndSortTestCaseGen = for
    size <- Gen.choose(3, 5)
    selectedSkus <- nDistinct(size, skuGen)
    otherSkus <- nDistinctExcluding(size, skuGen, selectedSkus)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories <- nDistinctExcluding(3, categoryGen, selectedCategories)
    quantityRange <- rangeIntGen(300, 600)
    selectedStockAvailabilities <- selectedSkus.zip(selectedCategories).traverse {
      case (sku, category) =>
        stockAvailabilityGen(
          skuGen = sku,
          categoryGen = category,
          quantityGen = Gen
            .choose(quantityRange.start, quantityRange.end)
            .map(StockAvailability.Quantity.applyUnsafe),
        )
    }
    otherStockAvailabilities <- otherSkus.zip(otherCategories).traverse { case (sku, category) =>
      stockAvailabilityGen(
        skuGen = sku,
        categoryGen = category,
        quantityGen = Gen
          .frequency(
            1 -> Gen.choose(0, quantityRange.start - 1),
            1 -> Gen.choose(quantityRange.end + 1, 1_000),
          )
          .map(StockAvailability.Quantity.applyUnsafe),
      )
    }
    stockAvailabilities = Random.shuffle(selectedStockAvailabilities ++ otherStockAvailabilities)
    filter = And(
      In(selectedCategories*),
      Between(quantityRange.map(StockAvailability.Quantity.applyUnsafe)),
    )
    sortWithFunction <- Gen.oneOf(
      (
        Ascending(sortableCategory),
        (xs: List[StockAvailability]) => xs.sortBy(_.category.toLowerCase),
      ),
      (
        Descending(sortableCategory),
        (xs: List[StockAvailability]) => xs.sortBy(_.category.toLowerCase).reverse,
      ),
      (Ascending(sortableQuantity), (xs: List[StockAvailability]) => xs.sortBy(_.quantity)),
      (Descending(sortableQuantity), (xs: List[StockAvailability]) => xs.sortBy(_.quantity).reverse),
    )
    (sort, sortingFunction) = sortWithFunction
    roundedUnits = selectedStockAvailabilities
      .map: stockAvailability =>
        val roundedPrice = stockAvailability.unitPrice
          .in(euroContext.defaultCurrency)
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        stockAvailability.copy(unitPrice = roundedPrice)
    expected = sortingFunction(roundedUnits)
    diff = if sort == NoSort then sortBySku else identity[List[StockAvailability]]
  yield TestCase(stockAvailabilities, filter, sort, expected, diff)
