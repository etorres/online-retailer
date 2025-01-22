package es.eriktorr
package electronics

import commons.api.Wire.{unWire, wire}
import commons.spec.CollectionGenerators.nDistinct
import electronics.ElectronicDeviceGenerators.{electronicDeviceGen, idGen}
import electronics.ProtobufWires.given
import electronics.ProtobufWiresSuite.{testCaseGen, TestCase}

import cats.implicits.toTraverseOps
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalacheck.cats.implicits.given

final class ProtobufWiresSuite extends ScalaCheckSuite:
  property("protobuf encoding is reversible"):
    forAll(testCaseGen):
      case TestCase(garments) =>
        val protobufGarments = garments.wire
        val obtained = protobufGarments.unWire
        assertEquals(obtained, garments)

object ProtobufWiresSuite:
  final private case class TestCase(garments: List[ElectronicDevice])

  final private val testCaseGen = for
    size <- Gen.choose(1, 5)
    ids <- nDistinct(size, idGen)
    garments <- ids.traverse(id => electronicDeviceGen(id))
  yield TestCase(garments)
