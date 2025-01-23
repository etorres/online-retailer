package es.eriktorr
package stock

import commons.api.Wire.{unWire, wire}
import commons.spec.CollectionGenerators.nDistinct
import stock.ProtobufWires.given
import stock.ProtobufWiresSuite.{testCaseGen, TestCase}
import stock.StockAvailabilityGenerators.{skuGen, stockAvailabilityGen}

import cats.implicits.toTraverseOps
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalacheck.cats.implicits.given

final class ProtobufWiresSuite extends ScalaCheckSuite:
  property("protobuf encoding is reversible"):
    forAll(testCaseGen):
      case TestCase(stockAvailabilities) =>
        val protobufStockAvailabilities = stockAvailabilities.wire
        val obtained = protobufStockAvailabilities.unWire
        assertEquals(obtained, stockAvailabilities)

object ProtobufWiresSuite:
  final private case class TestCase(stockAvailabilities: List[StockAvailability])

  final private val testCaseGen = for
    size <- Gen.choose(1, 5)
    skus <- nDistinct(size, skuGen)
    stockAvailabilities <- skus.traverse(sku => stockAvailabilityGen(sku))
  yield TestCase(stockAvailabilities)
