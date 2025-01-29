package es.eriktorr
package clothing

import clothing.ClothingServiceSuite.{testCaseGen, TestCase}
import clothing.FakeClothingRepository.ClothingRepositoryState
import clothing.GarmentGenerators.*
import clothing.ProtobufWires.given
import clothing.protobuf.ClothingRequest.Filter.SearchTerm
import clothing.protobuf.ClothingRequest.{Filter, Sort}
import clothing.protobuf.{ClothingReply, ClothingRequest}
import commons.api.Wire.wire
import commons.spec.CollectionGenerators.nDistinct

import cats.effect.{IO, Ref}
import cats.implicits.{catsSyntaxOptionId, toTraverseOps}
import fs2.Stream
import io.grpc.Metadata
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class ClothingServiceSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should get garments"):
    forAllF(testCaseGen):
      case TestCase(garments, request, expected) =>
        val initialState = ClothingRepositoryState.empty.set(garments)
        (for
          logger <- Slf4jLogger.create[IO]
          stateRef <- Ref.of[IO, ClothingRepositoryState](initialState)
          clothingRepository = FakeClothingRepository(stateRef)
          clothingService = ClothingService(clothingRepository, chunkSize = 512)(using logger)
          obtained <- clothingService
            .sendClothingStream(Stream.emit[IO, ClothingRequest](request), Metadata())
            .compile
            .last
          finalState <- stateRef.get
        yield finalState -> obtained).assertEquals(initialState -> expected)

  test("should find a garment by its id"):
    fail("not implemented")

object ClothingServiceSuite:
  final private case class TestCase(
      garments: List[Garment],
      request: ClothingRequest,
      expected: Option[ClothingReply],
  )

  private val testCaseGen = for
    numIds <- Gen.choose(3, 5)
    ids <- nDistinct(numIds, idGen)
    garments <- ids.traverse(id => garmentGen(id))
    filter <- for
      fields <- Gen.someOf(
        Set(
          SearchTerm.Field.Category,
          SearchTerm.Field.Model,
          SearchTerm.Field.Size,
          SearchTerm.Field.Color,
        ),
      )
      searchTerms <- fields.toList.traverse(field =>
        for
          nunValues <- Gen.choose(1, 3)
          values <- Gen.nonEmptyListOf(field match
            case SearchTerm.Field.Category => categoryGen.map(_.toString)
            case SearchTerm.Field.Model => modelGen.map(_.toString)
            case SearchTerm.Field.Size => sizeGen.map(size => s"T_$size")
            case SearchTerm.Field.Color => colorGen.map(_.toString))
        yield SearchTerm(field, values),
      )
    yield Filter(searchTerms).some
    sort <- for
      field <- Gen.const(Sort.Field.Price)
      order <- Gen.oneOf(Sort.Order.Ascending, Sort.Order.Descending)
    yield Sort(field, order).some
    request = ClothingRequest(filter, sort)
    reply = ClothingReply(garments.wire)
    expected = Some(reply)
  yield TestCase(garments, request, expected)
