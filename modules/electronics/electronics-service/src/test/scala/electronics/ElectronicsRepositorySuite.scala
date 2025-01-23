package es.eriktorr
package electronics

import commons.market.EuroMoneyContext.given
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.{Ascending, Descending, NoSort}
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.PostgresSuite
import commons.spec.RangeGenerators.rangeDoubleGen
import electronics.ElectronicDeviceGenerators.{categoryGen, electronicDeviceGen, idGen}
import electronics.ElectronicsRepositorySuite.{
  filterAndSortTestCaseGen,
  selectAllTestCaseGen,
  TestCase,
}
import electronics.db.ElectronicDeviceConnection.given
import electronics.db.TestElectronicsRepository

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import squants.energy.Watts

import scala.util.Random

final class ElectronicsRepositorySuite extends PostgresSuite:
  test("should list all electronic devices"):
    testWith(selectAllTestCaseGen)

  test("should filter and sort electronic devices"):
    testWith(filterAndSortTestCaseGen)

  private def testWith(testCaseGen: Gen[TestCase]) =
    forAllF(testCaseGen):
      case TestCase(electronicDevices, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testElectronicsRepository = TestElectronicsRepository(transactor)
          val testee = ElectronicsRepository.Postgres(transactor)
          (for
            _ <- electronicDevices.traverse_(testElectronicsRepository.add)
            obtained <- testee.selectElectronicDevicesBy(filter, sort).compile.toList
          yield diff(obtained)).assertEquals(diff(expected))

object ElectronicsRepositorySuite:
  final private case class TestCase(
      electronicDevices: List[ElectronicDevice],
      filter: Filter,
      sort: Sort,
      expected: List[ElectronicDevice],
      diff: List[ElectronicDevice] => List[ElectronicDevice],
  )

  private val sortById = (xs: List[ElectronicDevice]) => xs.sortBy(_.id)

  private val selectAllTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    electronicDevices <- ids.traverse(id => electronicDeviceGen(id))
    expected = electronicDevices.map: electronicDevice =>
      val roundedPower = electronicDevice.powerConsumption
        .in(Watts)
        .map(value => BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue)
      val roundedPrice = electronicDevice.price
        .in(euroContext.defaultCurrency)
        .map(amount => BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue)
      electronicDevice.copy(powerConsumption = roundedPower, price = roundedPrice)
  yield TestCase(electronicDevices, NoFilter, NoSort, expected, sortById)

  private val filterAndSortTestCaseGen = for
    size <- Gen.choose(3, 5)
    selectedIds <- nDistinct(size, idGen)
    otherIds <- nDistinctExcluding(size, idGen, selectedIds)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories = Category.values.toList.diff(selectedCategories)
    priceRange <- rangeDoubleGen(10d, 1_000d)
    selectedElectronicDevices <- selectedIds.traverse(id =>
      electronicDeviceGen(
        idGen = id,
        categoryGen = Gen.oneOf(selectedCategories),
        priceGen =
          Gen.choose(priceRange.start, priceRange.end).map(euroContext.defaultCurrency.apply),
      ),
    )
    otherElectronicDevices <- otherIds.traverse(id =>
      electronicDeviceGen(
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
    roundedUnits = selectedElectronicDevices
      .map: electronicDevice =>
        val roundedPower = electronicDevice.powerConsumption
          .in(Watts)
          .map(value => BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue)
        val roundedPrice = electronicDevice.price
          .in(euroContext.defaultCurrency)
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        electronicDevice.copy(
          powerConsumption = roundedPower,
          price = roundedPrice,
        )
    expected = sortingFunction(roundedUnits)
    diff = if sort == NoSort then sortById else identity[List[ElectronicDevice]]
  yield TestCase(electronicDevices, filter, sort, expected, diff)
