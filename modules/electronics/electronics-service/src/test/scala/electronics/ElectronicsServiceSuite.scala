package es.eriktorr
package electronics

import commons.api.Wire.wire
import commons.spec.CollectionGenerators.nDistinct
import electronics.ElectronicDeviceGenerators.{categoryGen, electronicDeviceGen, idGen, modelGen}
import electronics.ElectronicsServiceSuite.{testCaseGen, TestCase}
import electronics.FakeElectronicsRepository.ElectronicsRepositoryState
import electronics.ProtobufWires.given
import electronics.protobuf.ElectronicsRequest.Filter.SearchTerm
import electronics.protobuf.ElectronicsRequest.{Filter, Sort}
import electronics.protobuf.{ElectronicsReply, ElectronicsRequest}

import cats.effect.{IO, Ref}
import cats.implicits.{catsSyntaxOptionId, toTraverseOps}
import fs2.Stream
import io.grpc.Metadata
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class ElectronicsServiceSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should get electronic devices"):
    forAllF(testCaseGen):
      case TestCase(electronicDevices, request, expected) =>
        val initialState = ElectronicsRepositoryState.empty.set(electronicDevices)
        (for
          logger <- Slf4jLogger.create[IO]
          stateRef <- Ref.of[IO, ElectronicsRepositoryState](initialState)
          clothingRepository = FakeElectronicsRepository(stateRef)
          clothingService = ElectronicsService(clothingRepository, chunkSize = 512)(using logger)
          obtained <- clothingService
            .sendElectronicsStream(Stream.emit[IO, ElectronicsRequest](request), Metadata())
            .compile
            .last
          finalState <- stateRef.get
        yield finalState -> obtained).assertEquals(initialState -> expected)

  test("should find an electronic device by its id"):
    fail("not implemented")

object ElectronicsServiceSuite:
  final private case class TestCase(
      electronicDevices: List[ElectronicDevice],
      request: ElectronicsRequest,
      expected: Option[ElectronicsReply],
  )

  final private val testCaseGen = for
    numIds <- Gen.choose(3, 5)
    ids <- nDistinct(numIds, idGen)
    electronicDevices <- ids.traverse(id => electronicDeviceGen(id))
    filter <- for
      fields <- Gen.someOf(
        Set(
          SearchTerm.Field.Category,
          SearchTerm.Field.Model,
        ),
      )
      searchTerms <- fields.toList.traverse(field =>
        for
          nunValues <- Gen.choose(1, 3)
          values <- Gen.nonEmptyListOf(field match
            case SearchTerm.Field.Category => categoryGen.map(_.toString)
            case SearchTerm.Field.Model => modelGen.map(_.toString))
        yield SearchTerm(field, values),
      )
    yield Filter(searchTerms).some
    sort <- for
      field <- Gen.oneOf(Sort.Field.PowerConsumption, Sort.Field.Price)
      order <- Gen.oneOf(Sort.Order.Ascending, Sort.Order.Descending)
    yield Sort(field, order).some
    request = ElectronicsRequest(filter, sort)
    reply = ElectronicsReply(electronicDevices.wire)
    expected = Some(reply)
  yield TestCase(electronicDevices, request, expected)
