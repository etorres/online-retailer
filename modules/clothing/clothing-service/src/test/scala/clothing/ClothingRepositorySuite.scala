package es.eriktorr
package clothing

import clothing.ClothingRepositorySuite.{filterAndSortTestCaseGen, selectAllTestCaseGen, TestCase}
import clothing.GarmentGenerators.{categoryGen, colorGen, garmentGen, idGen}
import clothing.db.GarmentConnection.{sortablePrice, given}
import clothing.db.TestClothingRepository
import commons.market.EuroMoneyContext.given
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.{Ascending, Descending, NoSort}
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.PostgresSuite
import commons.spec.RangeGenerators.rangeDoubleGen

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF

import scala.util.Random

final class ClothingRepositorySuite extends PostgresSuite:
  test("should list all garments"):
    testWith(selectAllTestCaseGen)

  test("should filter and sort garments"):
    testWith(filterAndSortTestCaseGen)

  private def testWith(testCaseGen: Gen[TestCase]) =
    forAllF(testCaseGen):
      case TestCase(garments, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testClothingRepository = TestClothingRepository(transactor)
          val testee = ClothingRepository.Postgres(transactor)
          (for
            _ <- garments.traverse_(testClothingRepository.add)
            obtained <- testee.selectGarmentsBy(filter, sort).compile.toList
          yield diff(obtained)).assertEquals(diff(expected))

object ClothingRepositorySuite:
  final private case class TestCase(
      garments: List[Garment],
      filter: Filter,
      sort: Sort,
      expected: List[Garment],
      diff: List[Garment] => List[Garment],
  )

  private val sortById = (xs: List[Garment]) => xs.sortBy(_.id)

  private val selectAllTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    garments <- ids.traverse(id => garmentGen(id))
    expected = garments.map: garment =>
      val roundedPrice = garment.price
        .in(euroContext.defaultCurrency)
        .map(amount => BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue)
      garment.copy(price = roundedPrice)
  yield TestCase(garments, NoFilter, NoSort, expected, sortById)

  private val filterAndSortTestCaseGen = for
    size <- Gen.choose(3, 5)
    selectedIds <- nDistinct(size, idGen)
    otherIds <- nDistinctExcluding(size, idGen, selectedIds)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories = Category.values.toList.diff(selectedCategories)
    selectedColors <- nDistinct(3, colorGen)
    otherColors = Color.values.toList.diff(selectedColors)
    priceRange <- rangeDoubleGen(10d, 1_000d)
    selectedGarments <- selectedIds.traverse(id =>
      garmentGen(
        idGen = id,
        categoryGen = Gen.oneOf(selectedCategories),
        colorGen = Gen.oneOf(selectedColors),
        priceGen =
          Gen.choose(priceRange.start, priceRange.end).map(euroContext.defaultCurrency.apply),
      ),
    )
    otherGarments <- otherIds.traverse(id =>
      garmentGen(
        idGen = id,
        categoryGen = Gen.oneOf(otherCategories),
        colorGen = Gen.oneOf(otherColors),
        priceGen = Gen.frequency(
          1 -> Gen.choose(1d, priceRange.start - 1d).map(euroContext.defaultCurrency.apply),
          1 -> Gen.choose(priceRange.end + 1d, 2_000d).map(euroContext.defaultCurrency.apply),
        ),
      ),
    )
    garments = Random.shuffle(selectedGarments ++ otherGarments)
    filter = And(
      In(selectedCategories*),
      In(selectedColors*),
      Between(priceRange.map(euroContext.defaultCurrency.apply)),
    )
    sort <- Gen.oneOf(Ascending(sortablePrice), Descending(sortablePrice))
    roundedUnits = selectedGarments
      .map: garment =>
        val roundedPrice = garment.price
          .in(euroContext.defaultCurrency)
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        garment.copy(price = roundedPrice)
    expected = sort match
      case Ascending(sortable) => roundedUnits.sortBy(_.price.amount)
      case Descending(sortable) => roundedUnits.sortBy(_.price.amount).reverse
      case NoSort => roundedUnits
    diff = if sort == NoSort then sortById else identity[List[Garment]]
  yield TestCase(garments, filter, sort, expected, diff)
