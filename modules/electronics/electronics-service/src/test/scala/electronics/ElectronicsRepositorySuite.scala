package es.eriktorr
package electronics

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
import electronics.ElectronicDeviceGenerators.{categoryGen, idGen}
import electronics.ElectronicsRepositorySuite.{
  filterAndSortTestCaseGen,
  findElectronicDeviceByIdTestCaseGen,
  selectAllTestCaseGen,
  TestCase,
}
import electronics.db.ElectronicDeviceRow
import electronics.db.ElectronicDeviceRowGenerators.electronicDeviceRowGen
import electronics.db.ElectronicDeviceTable.given
import electronics.db.TestElectronicsRepository

import cats.effect.IO
import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import squants.energy.Watts

import scala.util.Random

final class ElectronicsRepositorySuite extends PostgresSuite:
  test("should find an electronic device by its id"):
    testWith(
      findElectronicDeviceByIdTestCaseGen,
      (testee, filter, _) => testee.findElectronicDeviceBy(filter.to).value,
    )

  test("should list all electronic devices"):
    testWith(
      selectAllTestCaseGen,
      (testee, filter, sort) => testee.selectElectronicDevicesBy(filter, sort).compile.toList,
    )

  test("should filter and sort electronic devices"):
    testWith(
      filterAndSortTestCaseGen,
      (testee, filter, sort) => testee.selectElectronicDevicesBy(filter, sort).compile.toList,
    )

  private def testWith[A, B <: Filter](
      testCaseGen: Gen[TestCase[A, B]],
      run: (ElectronicsRepository, B, Sort) => IO[A],
  ) =
    forAllF(testCaseGen):
      case TestCase(salesTaxRows, electronicDeviceRows, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testSalesTaxRepository = TestSalesTaxRepository(transactor)
          val testElectronicsRepository = TestElectronicsRepository(transactor)
          val testee = ElectronicsRepository.Postgres(transactor)
          (for
            _ <- salesTaxRows.traverse_(testSalesTaxRepository.add)
            _ <- electronicDeviceRows.traverse_(testElectronicsRepository.add)
            obtained <- run(testee, filter, sort)
          yield diff(obtained)).assertEquals(diff(expected))

object ElectronicsRepositorySuite:
  final private case class TestCase[A, B <: Filter](
      salesTaxRows: List[SalesTaxRow],
      electronicDeviceRows: List[ElectronicDeviceRow],
      filter: B,
      sort: Sort,
      expected: A,
      diff: A => A,
  )

  private val sortById = (xs: List[ElectronicDevice]) => xs.sortBy(_.id)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def unRowWith(salesTaxToRate: Map[SalesTax, SalesTaxRow.Rate]) =
    (electronicDeviceRow: ElectronicDeviceRow) =>
      toStandardUnits(
        ElectronicDeviceRow
          .unRowWith(electronicDeviceRow, salesTaxToRate)
          .getOrElse(throw IllegalArgumentException("Failed to un-row electronic device")),
      )

  private val toStandardUnits = (electronicDevice: ElectronicDevice) =>
    val roundedPower = electronicDevice.powerConsumption
      .in(Watts)
      .map(value => BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue)
    val roundedPrice = electronicDevice.price
      .in(euroContext.defaultCurrency)
      .map(amount => BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue)
    val roundedTax = ElectronicDevice.Tax.applyUnsafe(
      BigDecimal(electronicDevice.tax).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue,
    )
    electronicDevice.copy(powerConsumption = roundedPower, price = roundedPrice, tax = roundedTax)

  private val findElectronicDeviceByIdTestCaseGen = for
    salesTaxes <- salesTaxRowsGen
    salesTaxToRate = salesTaxes.map(x => x.tax -> x.rate).toMap
    selectedId <- idGen
    selectedElectronicDevice <- electronicDeviceRowGen(selectedId)
    size <- Gen.choose(3, 5)
    otherIds <- nDistinctExcluding(size, idGen, Set(selectedId))
    otherElectronicDevices <- otherIds.traverse(id => electronicDeviceRowGen(id))
    expected = Option(selectedElectronicDevice).map(unRowWith(salesTaxToRate))
  yield TestCase(
    salesTaxes,
    selectedElectronicDevice :: otherElectronicDevices,
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
    electronicDevices <- ids.traverse(id => electronicDeviceRowGen(id))
    expected = electronicDevices.map(unRowWith(salesTaxToRate))
  yield TestCase(salesTaxes, electronicDevices, NoFilter, NoSort, expected, sortById)

  private val filterAndSortTestCaseGen = for
    salesTaxes <- salesTaxRowsGen
    salesTaxToRate = salesTaxes.map(x => x.tax -> x.rate).toMap
    size <- Gen.choose(3, 5)
    selectedIds <- nDistinct(size, idGen)
    otherIds <- nDistinctExcluding(size, idGen, selectedIds)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories = Category.values.toList.diff(selectedCategories)
    priceRange <- rangeDoubleGen(10d, 1_000d)
    selectedElectronicDevices <- selectedIds.traverse(id =>
      electronicDeviceRowGen(
        idGen = id,
        categoryGen = Gen.oneOf(selectedCategories),
        priceGen =
          Gen.choose(priceRange.start, priceRange.end).map(euroContext.defaultCurrency.apply),
      ),
    )
    otherElectronicDevices <- otherIds.traverse(id =>
      electronicDeviceRowGen(
        idGen = id,
        categoryGen = Gen.oneOf(otherCategories),
        priceGen = Gen
          .frequency(
            1 -> Gen.choose(1d, priceRange.start - 1d),
            1 -> Gen.choose(priceRange.end + 1d, 2_000d),
          )
          .map(euroContext.defaultCurrency.apply),
      ),
    )
    electronicDevices = Random.shuffle(selectedElectronicDevices ++ otherElectronicDevices)
    filter = And(
      In(selectedCategories*),
      Between(priceRange.map(euroContext.defaultCurrency.apply)),
    )
    sortWithFunction <- Gen.oneOf(
      (Ascending(sortablePower), (xs: List[ElectronicDevice]) => xs.sortBy(_.powerConsumption)),
      (
        Descending(sortablePower),
        (xs: List[ElectronicDevice]) => xs.sortBy(_.powerConsumption).reverse,
      ),
      (Ascending(sortablePrice), (xs: List[ElectronicDevice]) => xs.sortBy(_.price)),
      (Descending(sortablePrice), (xs: List[ElectronicDevice]) => xs.sortBy(_.price).reverse),
    )
    (sort, sortingFunction) = sortWithFunction
    roundedUnits = selectedElectronicDevices.map(unRowWith(salesTaxToRate))
    expected = sortingFunction(roundedUnits)
    diff = if sort == NoSort then sortById else identity[List[ElectronicDevice]]
  yield TestCase(salesTaxes, electronicDevices, filter, sort, expected, diff)
