package es.eriktorr
package products

import commons.spec.CollectionGenerators.{nDistinct, nDistinctExcluding}
import commons.spec.GenSyntax.sampleWithSeed
import products.Product.{PowerUnitArgs, Queries}
import products.ProductSuite.{findProductByIdTestCaseGen, getProductsTestCaseGen, TestCase}
import products.ProductGenerators.{idGen, productGen}

import caliban.ResponseValue.{ListValue, ObjectValue}
import caliban.Value.{FloatValue, StringValue}
import caliban.{graphQL, render, GraphQLResponse, RootResolver}
import cats.implicits.toTraverseOps
import munit.ZSuite
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import zio.ZIO

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class ProductSuite extends ZSuite:
  test("should render GraphQL SDL from the main type"):
    val schema = render[Queries]
    assert(schema.nonEmpty)

  testZ("should find a product by its id"):
    testWith(findProductByIdTestCaseGen)

  testZ("should get products"):
    testWith(getProductsTestCaseGen)

  private def testWith(testCaseGen: Gen[TestCase]) =
    val testCase = testCaseGen.sampleWithSeed()
    val productService = FakeProductService(testCase.products)
    val interpreter = graphQL(
      RootResolver(
        Queries(
          products = args =>
            productService.findProductsBy(
              args.lookup.searchTerms,
              args.lookup.ranges,
              args.lookup.sort,
            ),
          product = args => productService.productById(args.id),
        ),
      ),
    ).interpreterUnsafe
    val request = interpreter.execute(testCase.query)
    ZIO
      .blocking(request)
      .map(assertEquals(_, testCase.expected))

object ProductSuite:
  final private case class TestCase(
      products: List[Product],
      query: String,
      expected: GraphQLResponse[?],
  )

  private def objectValueFrom(product: Product) =
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
        ObjectValue(
          List(
            "category" -> StringValue(category),
            "__typename" -> StringValue("ElectronicDevice"),
            "powerConsumption" -> FloatValue.DoubleNumber(
              powerConsumption(PowerUnitArgs(None)),
            ),
          ),
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
        ObjectValue(
          List(
            "category" -> StringValue(category),
            "__typename" -> StringValue("Garment"),
            "color" -> StringValue(color),
            "size" -> StringValue(size),
          ),
        )

  private val findProductByIdTestCaseGen = for
    selectedId <- idGen
    selectedProduct <- productGen(idGen = selectedId)
    size <- Gen.choose(3, 5)
    otherIds <- nDistinctExcluding(size, idGen, Set(selectedId))
    otherProducts <- otherIds.traverse(id => productGen(idGen = id))
    expected = GraphQLResponse(
      ObjectValue(List("product" -> objectValueFrom(selectedProduct))),
      List.empty,
    )
  yield TestCase(
    selectedProduct :: otherProducts,
    s"""query GetProductById {
       |  product(id: $selectedId) {
       |    category
       |    __typename
       |    ... on ElectronicDevice {
       |      powerConsumption
       |    }
       |    ... on Garment {
       |      color
       |      size
       |    }
       |  }
       |}""".stripMargin,
    expected,
  )

  private val getProductsTestCaseGen = for
    size <- Gen.choose(3, 5)
    ids <- nDistinct(size, idGen)
    products <- ids.traverse(id => productGen(idGen = id))
    expected = GraphQLResponse(
      ObjectValue(List("products" -> ListValue(products.map(x => objectValueFrom(x))))),
      List.empty,
    )
  yield TestCase(
    products,
    s"""query SelectProducts {
       |  products(
       |    lookup: {
       |      searchTerms: []
       |      ranges: []
       |    }
       |  ) {
       |    category
       |    __typename
       |    ... on ElectronicDevice {
       |      powerConsumption
       |    }
       |    ... on Garment {
       |      color
       |      size
       |    }
       |  }
       |}""".stripMargin,
    expected,
  )
