package es.eriktorr
package clothing

import clothing.ClothingRepositorySuite.{filterAndSortTestCaseGen, selectAllTestCaseGen, TestCase}
import clothing.GarmentGenerators.{categoryGen, colorGen, garmentGen, idGen}
import clothing.db.GarmentConnection.{sortablePrice, given}
import clothing.db.TestClothingRepository
import commons.market.EuroMoneyContext.given
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.NoFilter
import commons.query.Sort.{Ascending, Descending, NoSort}
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.PostgresSuite

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
      case TestCase(garments, filter, sort, expected) =>
        testTransactor.resource.use: transactor =>
          val testClothingRepository = TestClothingRepository(transactor)
          val testee = ClothingRepository.Postgres(transactor)
          (for
            _ <- garments.traverse_(testClothingRepository.add)
            obtained <- testee.selectGarmentsBy(filter, sort).compile.toList
          yield obtained).assertEquals(expected)

object ClothingRepositorySuite:
  final private case class TestCase(
      garments: List[Garment],
      filter: Filter,
      sort: Sort,
      expected: List[Garment],
  )

  private val selectAllTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    garments <- ids.traverse(id => garmentGen(id))
    expected = garments.map: garment =>
      val priceInEur = garment.price.in(euroContext.defaultCurrency)
      val rounded = priceInEur.amount.setScale(5, BigDecimal.RoundingMode.HALF_UP)
      garment.copy(price = euroContext.defaultCurrency(rounded))
  yield TestCase(garments, NoFilter, NoSort, expected)

  private val filterAndSortTestCaseGen = for
    size <- Gen.choose(3, 5)
    selectedIds <- nDistinct(size, idGen)
    otherIds <- nDistinctExcluding(size, idGen, selectedIds)
    selectedCategories <- nDistinct(3, categoryGen)
    otherCategories = Category.values.toList.diff(selectedCategories)
    selectedColors <- nDistinct(3, colorGen)
    otherColors = Color.values.toList.diff(selectedColors)
    selectedGarments <- selectedIds.traverse(id =>
      garmentGen(
        idGen = id,
        categoryGen = Gen.oneOf(selectedCategories),
        colorGen = Gen.oneOf(selectedColors),
      ),
    )
    otherGarments <- otherIds.traverse(id =>
      garmentGen(
        idGen = id,
        categoryGen = Gen.oneOf(otherCategories),
        colorGen = Gen.oneOf(otherColors),
      ),
    )
    garments = Random.shuffle(selectedGarments ++ otherGarments)
    filter = And(In(selectedCategories*), In(selectedColors*))
    sort <- Gen.oneOf(Ascending(sortablePrice), Descending(sortablePrice))
    pricesInEur = selectedGarments
      .map: garment =>
        val priceInEur = garment.price.in(euroContext.defaultCurrency)
        val rounded = priceInEur.amount.setScale(5, BigDecimal.RoundingMode.HALF_UP)
        garment.copy(price = euroContext.defaultCurrency(rounded))
      .sortBy(_.price.amount)
    expected = sort match
      case Ascending(sortable) => pricesInEur.sortBy(_.price.amount)
      case Descending(sortable) => pricesInEur.sortBy(_.price.amount).reverse
      case NoSort => pricesInEur
  yield TestCase(garments, filter, sort, expected)
