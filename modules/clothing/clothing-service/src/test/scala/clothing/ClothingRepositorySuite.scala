package es.eriktorr
package clothing

import clothing.ClothingRepositorySuite.{testCaseGen, TestCase}
import clothing.GarmentGenerators.{garmentGen, idGen}
import clothing.db.TestClothingRepository
import commons.market.EuroMoneyContext.given
import commons.query.{Filter, Sort}
import commons.spec.CollectionGenerators.nDistinct
import commons.spec.PostgresSuite

import cats.implicits.{toFoldableOps, toTraverseOps}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF

final class ClothingRepositorySuite extends PostgresSuite:
  test("should list all garments"):
    forAllF(testCaseGen):
      case TestCase(garments, filter, sort, expected) =>
        testTransactor.resource.use: transactor =>
          val testClothingRepository = TestClothingRepository(transactor)
          val testee = ClothingRepository.Postgres(transactor)
          (for
            _ <- garments.traverse_(testClothingRepository.add)
            obtained <- testee.findGarmentsBy(filter, sort)
          yield obtained).assertEquals(expected)

  test("should filter and sort garments"):
    fail("not implemented")

object ClothingRepositorySuite:
  final private case class TestCase(
      garments: List[Garment],
      filter: Filter,
      sort: Sort,
      expected: List[Garment],
  )

  private val testCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    garments <- ids.traverse(id => garmentGen(id))
    expected = garments.map: garment =>
      val priceInEur = garment.price.in(euroContext.defaultCurrency)
      val rounded = priceInEur.amount.setScale(5, BigDecimal.RoundingMode.HALF_UP)
      garment.copy(price = euroContext.defaultCurrency(rounded))
  yield TestCase(garments, Filter.NoFilter, Sort.NoSort, expected)
