package es.eriktorr
package products.api

import clothing.{Category as ClothingCategory, Color, FakeClothingClient, Garment, Size}
import clothing.FakeClothingClient.ClothingClientState
import commons.market.EuroMoneyContext.given
import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import electronics.{Category as ElectronicsCategory, ElectronicDevice, FakeElectronicsClient}
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
import stock.StockAvailabilityGenerators.availableStock

import cats.effect.{IO, Ref}
import cats.implicits.toTraverseOps
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import squants.energy.Watts

final class ProductGatewaySuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should find a product by its id"):
    testWith(findProductByIdTestCaseGen, (testee, query) => testee.productById(query))

  test("should find products"):
    testWith(
      selectProductsTestCaseGen,
      (testee, query) =>
        val (searchTerms, ranges, maybeSort) = query
        testee.findProductsBy(searchTerms, ranges, maybeSort),
    )

  private def testWith[Q, R, E](
      testCaseGen: Gen[TestCase[Q, R, E]],
      run: (productGateway: ProductGateway[IO], query: Q) => IO[R],
  ) =
    forAllF(testCaseGen):
      case TestCase(electronicDevices, garments, stockAvailabilities, query, evaluate, expected) =>
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
        yield evaluate(obtained)).assertEquals(expected)

object ProductGatewaySuite:
  final private case class TestCase[Q, R, E](
      electronicDevices: List[ElectronicDevice],
      garments: List[Garment],
      stockAvailabilities: List[StockAvailability],
      query: Q,
      evaluate: R => E,
      expected: E,
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

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private val evaluateProduct = (product: Product) =>
    product match
      case Product.ElectronicDevice(
            id,
            category,
            model,
            powerConsumption,
            price,
            tax,
            description,
            launchDate,
            images,
          ) =>
        val roundedPower = Watts(powerConsumption(Product.PowerUnitArgs(None)))
          .map(value => BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue)
        val roundedPrice = euroContext
          .defaultCurrency(price(Product.CurrencyArgs(None)))
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        ElectronicDevice(
          ElectronicDevice.Id.applyUnsafe(id),
          ElectronicsCategory.values
            .find(_.name == category)
            .getOrElse(throw IllegalArgumentException(s"Unsupported category: $category")),
          ElectronicDevice.Model.applyUnsafe(model),
          roundedPower,
          roundedPrice,
          ElectronicDevice.Tax.applyUnsafe(tax),
          ElectronicDevice.Description.applyUnsafe(description),
          launchDate,
          images.map(image => ElectronicDevice.Image.applyUnsafe(image)),
        )
      case Product.Garment(
            id,
            category,
            model,
            size,
            color,
            price,
            tax,
            description,
            launchDate,
            images,
          ) =>
        val roundedPrice = euroContext
          .defaultCurrency(price(Product.CurrencyArgs(None)))
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        Garment(
          Garment.Id.applyUnsafe(id),
          ClothingCategory.values
            .find(_.name == category)
            .getOrElse(throw IllegalArgumentException(s"Unsupported category: $category")),
          Garment.Model.applyUnsafe(model),
          Size.valueOf(size),
          Color.valueOf(color),
          roundedPrice,
          Garment.Tax.applyUnsafe(tax),
          Garment.Description.applyUnsafe(description),
          launchDate,
          images.map(image => Garment.Image.applyUnsafe(image)),
        )

  private val evaluateProductOpt = (maybeProduct: Option[Product]) =>
    maybeProduct.map(evaluateProduct)

  private val evaluateProductList = (products: List[Product]) =>
    products.sortBy(_.id).map(evaluateProduct)

  private val toStandardUnits = (product: ElectronicDevice | Garment) =>
    product match
      case electronicDevice: ElectronicDevice =>
        val roundedPower = electronicDevice.powerConsumption
          .in(Watts)
          .map(value => BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue)
        val roundedPrice = electronicDevice.price
          .in(euroContext.defaultCurrency)
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        electronicDevice.copy(powerConsumption = roundedPower, price = roundedPrice)
      case garment: Garment =>
        val roundedPrice = garment.price
          .in(euroContext.defaultCurrency)
          .map(amount =>
            BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).doubleValue,
          )
        garment.copy(price = roundedPrice)

  private val findProductByIdTestCaseGen = for
    selectedId <- idGen
    selectedProduct <- productGenWithSeed(idGen = selectedId)
    size <- Gen.choose(3, 5)
    otherIds <- nDistinctExcluding(size, idGen, Set(selectedId))
    otherProducts <- otherIds.traverse(id => productGenWithSeed(idGen = id))
    allProducts = selectedProduct :: otherProducts
    expected = Option(selectedProduct._1).map(toStandardUnits)
  yield TestCase(
    electronicDevicesFrom(allProducts),
    garmentsFrom(allProducts),
    List.empty,
    selectedId,
    evaluateProductOpt,
    expected,
  )

  private val selectProductsTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    products <- ids.traverse(id => productGenWithSeed(idGen = id))
    stockAvailabilities <- products
      .map(_._2)
      .traverse(product => availableStock(StockAvailability.SKU.applyUnsafe(product.id)))
    expected = products.sortBy(_._2.id).map(_._1).map(toStandardUnits)
  yield TestCase(
    electronicDevicesFrom(products),
    garmentsFrom(products),
    stockAvailabilities,
    (List.empty[SearchTerm], List.empty[Range], Option.empty[Sort]),
    evaluateProductList,
    expected,
  )
