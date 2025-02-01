package es.eriktorr
package products.api

import clothing.{FakeClothingClient, Garment}
import clothing.FakeClothingClient.ClothingClientState
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import electronics.{ElectronicDevice, FakeElectronicsClient}
import electronics.FakeElectronicsClient.ElectronicsClientState
import products.Product
import products.Product.{Range, SearchTerm, Sort}
import products.ProductGenerators.{idGen, productGenWithSeed}
import products.api.ProductGatewaySuite.{
  findProductByIdTestCaseGen,
  selectProductsTestCaseGen,
  TestCase,
}
import stock.{FakeStockClient, StockAvailability}
import stock.FakeStockClient.StockClientState

import cats.effect.{IO, Ref}
import cats.implicits.toTraverseOps
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF

final class ProductGatewaySuite extends CatsEffectSuite with ScalaCheckEffectSuite:

  override def scalaCheckInitialSeed = "cWZQTlbZor5tYoRuuUfZuHcKX-47ISUJSzp4VWenuXF=" // TODO

  test("should find a product by its id"):
    testWith(findProductByIdTestCaseGen, (testee, query) => testee.productById(query))

  test("should filter and sort products"):
    testWith(
      selectProductsTestCaseGen,
      (testee, query) =>
        val (searchTerms, ranges, maybeSort) = query
        testee.findProductsBy(searchTerms, ranges, maybeSort),
    )

  private def testWith[A, B](
      testCaseGen: Gen[TestCase[A, B]],
      run: (productGateway: ProductGateway[IO], query: A) => IO[B],
  ) =
    forAllF(testCaseGen):
      case TestCase(electronicDevices, garments, stockAvailabilities, query, expected) =>
        (for
          clothingStateRef <- Ref.of[IO, ClothingClientState](
            ClothingClientState.empty.set(garments),
          )
          electronicsStateRef <- Ref.of[IO, ElectronicsClientState](
            ElectronicsClientState.empty.set(electronicDevices),
          )
          stockStateRef <- Ref.of[IO, StockClientState](
            StockClientState.empty.set(stockAvailabilities),
          )
          testee = ProductGateway.Grpc[IO](
            FakeClothingClient(clothingStateRef),
            FakeElectronicsClient(electronicsStateRef),
            FakeStockClient(stockStateRef),
          )
          obtained <- run(testee, query)
          _ =
            // TODO
            println(s" >> OBTAINED: $obtained")
            println(s" >> EXPECTED: $expected")
            // TODO
        yield obtained).assertEquals(expected)

object ProductGatewaySuite:
  final private case class TestCase[A, B](
      electronicDevices: List[ElectronicDevice],
      garments: List[Garment],
      stockAvailabilities: List[StockAvailability],
      query: A,
      expected: B,
  )

  private def electronicDevicesFrom(products: List[(ElectronicDevice | Garment, Product)]) =
    products
      .map:
        case (seed, _) =>
          seed match
            case x: ElectronicDevice => Some(x)
            case _ => None
      .collect { case Some(value) => value }

  private def garmentsFrom(products: List[(ElectronicDevice | Garment, Product)]) =
    products
      .map:
        case (seed, _) =>
          seed match
            case x: Garment => Some(x)
            case _ => None
      .collect { case Some(value) => value }

  private def productFrom(productWithSeed: (ElectronicDevice | Garment, Product)) =
    productWithSeed._2

  private val findProductByIdTestCaseGen = for
    selectedId <- idGen
    selectedProduct <- productGenWithSeed(idGen = selectedId)
    size <- Gen.choose(3, 5)
    otherIds <- nDistinctExcluding(size, idGen, Set(selectedId))
    otherProducts <- otherIds.traverse(id => productGenWithSeed(idGen = id))
    allProducts = selectedProduct :: otherProducts
    expected = Option(selectedProduct).map(productFrom)
  yield TestCase(
    electronicDevicesFrom(allProducts),
    garmentsFrom(allProducts),
    List.empty,
    selectedId,
    expected,
  )

  private val selectProductsTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    products <- ids.traverse(id => productGenWithSeed(idGen = id))
    expected = products.map(productFrom)
  yield TestCase(
    electronicDevicesFrom(products),
    garmentsFrom(products),
    List.empty, // TODO
    (List.empty[SearchTerm], List.empty[Range], Option.empty[Sort]),
    expected,
  )
