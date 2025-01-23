package es.eriktorr
package stock

import commons.api.Wire.wire
import commons.spec.CollectionGenerators.nDistinct
import stock.StockAvailabilityGenerators.{categoryGen, skuGen, stockAvailabilityGen}
import stock.StockServiceSuite.{testCaseGen, TestCase}
import stock.FakeStockRepository.StockRepositoryState
import stock.ProtobufWires.given
import stock.protobuf.StockRequest.Filter.SearchTerm
import stock.protobuf.StockRequest.{Filter, Sort}
import stock.protobuf.{StockReply, StockRequest}

import cats.effect.{IO, Ref}
import cats.implicits.{catsSyntaxOptionId, toTraverseOps}
import fs2.Stream
import io.grpc.Metadata
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.given
import org.scalacheck.effect.PropF.forAllF
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class StockServiceSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("should get stock availabilities"):
    forAllF(testCaseGen):
      case TestCase(stockAvailabilities, request, expected) =>
        val initialState = StockRepositoryState.empty.set(stockAvailabilities)
        (for
          logger <- Slf4jLogger.create[IO]
          stateRef <- Ref.of[IO, StockRepositoryState](initialState)
          stockRepository = FakeStockRepository(stateRef)
          stockService = StockService(stockRepository, chunkSize = 512)(using logger)
          obtained <- stockService
            .sendStockStream(Stream.emit[IO, StockRequest](request), Metadata())
            .compile
            .last
          finalState <- stateRef.get
        yield finalState -> obtained).assertEquals(initialState -> expected)

object StockServiceSuite:
  final private case class TestCase(
      stockAvailabilities: List[StockAvailability],
      request: StockRequest,
      expected: Option[StockReply],
  )

  final private val testCaseGen = for
    numSkus <- Gen.choose(3, 5)
    skus <- nDistinct(numSkus, skuGen)
    stockAvailabilities <- skus.traverse(sku => stockAvailabilityGen(sku))
    filter <- for
      field <- Gen.const(SearchTerm.Field.Category)
      searchTerms <- for
        nunValues <- Gen.choose(1, 3)
        values <- Gen.nonEmptyListOf(categoryGen.map(_.toString))
      yield SearchTerm(field, values)
    yield Filter(List(searchTerms)).some
    sort <- for
      field <- Gen.oneOf(Sort.Field.Category, Sort.Field.Quantity)
      order <- Gen.oneOf(Sort.Order.Ascending, Sort.Order.Descending)
    yield Sort(field, order).some
    request = StockRequest(filter, sort)
    reply = StockReply(stockAvailabilities.wire)
    expected = Some(reply)
  yield TestCase(stockAvailabilities, request, expected)
