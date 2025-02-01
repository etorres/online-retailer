package es.eriktorr
package clothing

import clothing.ClothingRepositorySuite.{
  filterAndSortTestCaseGen,
  findGarmentByIdTestCaseGen,
  selectAllTestCaseGen,
  TestCase,
}
import clothing.GarmentGenerators.{categoryGen, colorGen, idGen}
import clothing.db.GarmentTable.{sortablePrice, given}
import clothing.db.{GarmentRow, TestClothingRepository}
import commons.domain.DomainGenerators.salesTaxRowsGen
import commons.domain.{SalesTax, SalesTaxRow, TestSalesTaxRepository}
import commons.market.EuroMoneyContext.given
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.{Between, Equal}
import commons.query.Filter.NoFilter
import commons.query.Sort.{Ascending, Descending, NoSort}
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.PostgresSuite
import commons.spec.RangeGenerators.rangeDoubleGen

import cats.effect.IO
import cats.implicits.{toFoldableOps, toTraverseOps}
import es.eriktorr.clothing.db.GarmentRowGenerators.garmentRowGen
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF

import scala.util.Random

final class ClothingRepositorySuite extends PostgresSuite:
  test("should find a garment by its id"):
    testWith(
      findGarmentByIdTestCaseGen,
      (testee, filter, _) => testee.findGarmentBy(filter.to).value,
    )

  test("should list all garments"):
    testWith(
      selectAllTestCaseGen,
      (testee, filter, sort) => testee.selectGarmentsBy(filter, sort).compile.toList,
    )

  test("should filter and sort garments"):
    testWith(
      filterAndSortTestCaseGen,
      (testee, filter, sort) => testee.selectGarmentsBy(filter, sort).compile.toList,
    )

  private def testWith[A, B <: Filter](
      testCaseGen: Gen[TestCase[A, B]],
      run: (ClothingRepository, B, Sort) => IO[A],
  ) =
    forAllF(testCaseGen):
      case TestCase(salesTaxRows, garmentRows, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testSalesTaxRepository = TestSalesTaxRepository(transactor)
          val testClothingRepository = TestClothingRepository(transactor)
          val testee = ClothingRepository.Postgres(transactor)
          (for
            _ <- salesTaxRows.traverse_(testSalesTaxRepository.add)
            _ <- garmentRows.traverse_(testClothingRepository.add)
            obtained <- run(testee, filter, sort)
          yield diff(obtained)).assertEquals(diff(expected))

object ClothingRepositorySuite:
  final private case class TestCase[A, B <: Filter](
      salesTaxRows: List[SalesTaxRow],
      garmentRows: List[GarmentRow],
      filter: B,
      sort: Sort,
      expected: A,
      diff: A => A,
  )

  private val sortById = (xs: List[Garment]) => xs.sortBy(_.id)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def unRowWith(salesTaxToRate: Map[SalesTax, SalesTaxRow.Rate]) =
    (garmentRow: GarmentRow) =>
      toStandardUnits(
        GarmentRow
          .unRowWith(garmentRow, salesTaxToRate)
          .getOrElse(throw IllegalArgumentException("Failed to un-row garment")),
      )

  private val toStandardUnits = (garment: Garment) =>
    val roundedPrice = garment.price
      .in(euroContext.defaultCurrency)
      .map(amount => BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue)
    val roundedTax = Garment.Tax.applyUnsafe(
      BigDecimal(garment.tax).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue,
    )
    garment.copy(price = roundedPrice, tax = roundedTax)

  private val findGarmentByIdTestCaseGen = for
    salesTaxes <- salesTaxRowsGen
    salesTaxToRate = salesTaxes.map(x => x.tax -> x.rate).toMap
    selectedId <- idGen
    selectedGarment <- garmentRowGen(selectedId)
    size <- Gen.choose(3, 5)
    otherIds <- nDistinctExcluding(size, idGen, Set(selectedId))
    otherGarments <- otherIds.traverse(id => garmentRowGen(id))
    expected = Option(selectedGarment).map(unRowWith(salesTaxToRate))
  yield TestCase(
    salesTaxes,
    selectedGarment :: otherGarments,
    Equal(selectedId),
    NoSort,
    expected,
    identity,
  )

  private val selectAllTestCaseGen = for
    salesTaxes <- salesTaxRowsGen
    salesTaxToRate = salesTaxes.map(x => x.tax -> x.rate).toMap
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    garments <- ids.traverse(id => garmentRowGen(id))
    expected = garments.map(unRowWith(salesTaxToRate))
  yield TestCase(salesTaxes, garments, NoFilter, NoSort, expected, sortById)

  private val filterAndSortTestCaseGen = for
    salesTaxes <- salesTaxRowsGen
    salesTaxToRate = salesTaxes.map(x => x.tax -> x.rate).toMap
    size <- Gen.choose(3, 5)
    selectedIds <- nDistinct(size, idGen)
    otherIds <- nDistinctExcluding(size, idGen, selectedIds)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories = Category.values.toList.diff(selectedCategories)
    selectedColors <- nDistinct(3, colorGen)
    otherColors = Color.values.toList.diff(selectedColors)
    priceRange <- rangeDoubleGen(10d, 1_000d)
    selectedGarments <- selectedIds.traverse(id =>
      garmentRowGen(
        idGen = id,
        categoryGen = Gen.oneOf(selectedCategories),
        colorGen = Gen.oneOf(selectedColors),
        priceGen =
          Gen.choose(priceRange.start, priceRange.end).map(euroContext.defaultCurrency.apply),
      ),
    )
    otherGarments <- otherIds.traverse(id =>
      garmentRowGen(
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
    roundedUnits = selectedGarments.map(unRowWith(salesTaxToRate))
    expected = sort match
      case Ascending(sortable) => roundedUnits.sortBy(_.price.amount)
      case Descending(sortable) => roundedUnits.sortBy(_.price.amount).reverse
      case NoSort => roundedUnits
    diff = if sort == NoSort then sortById else identity[List[Garment]]
  yield TestCase(salesTaxes, garments, filter, sort, expected, diff)
