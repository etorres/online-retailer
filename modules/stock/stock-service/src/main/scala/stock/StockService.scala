package es.eriktorr
package stock

import commons.api.Wire.wire
import commons.query.Column.Filterable
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.NoSort
import commons.query.{Filter, Sort}
import stock.ProtobufWires.given
import stock.db.StockAvailabilityTable.given
import stock.protobuf.StockRequest.Filter.SearchTerm.Field as SearchTermField
import stock.protobuf.StockRequest.Sort.{Field as SortField, Order as SortOrder}
import stock.protobuf.{StockFs2Grpc, StockReply, StockRequest}

import cats.collections.Range
import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.{catsSyntaxTuple2Semigroupal, toTraverseOps}
import doobie.Put
import fs2.Stream
import io.grpc.{Metadata, ServerServiceDefinition}
import org.typelevel.log4cats.Logger

final class StockService(stockRepository: StockRepository, chunkSize: Int)(using
    logger: Logger[IO],
) extends StockFs2Grpc[IO, Metadata]:
  override def sendStockStream(
      request: Stream[IO, StockRequest],
      context: Metadata,
  ): Stream[IO, StockReply] = request.flatMap: stockRequest =>
    for
      filter <- filterFrom(stockRequest)
      sort <- sortFrom(stockRequest)
      stockAvailabilities <- stockRepository
        .selectStockAvailabilitiesBy(filter, sort, chunkSize)
        .chunkMin(chunkSize, allowFewerTotal = true)
        .map(_.toList)
    yield StockReply(stockAvailabilities.wire)

  private def filterFrom(stockRequest: StockRequest): Stream[IO, Filter] =
    stockRequest.filter
      .traverse: filterRequest =>
        for
          searchTerms <- searchTermsFrom(filterRequest)
          quantityRange <- quantityRangeFrom(filterRequest)
        yield And(searchTerms ++ quantityRange*)
      .map(_.getOrElse(NoFilter))

  private def searchTermsFrom(filterRequest: StockRequest.Filter): Stream[IO, Seq[Filter]] =
    Stream.eval(
      filterRequest.searchTerms
        .traverse: searchTerm =>
          searchTerm.field match
            case SearchTermField.Category =>
              searchTermFrom(searchTerm, StockAvailability.Category.option, "category")
            case SearchTermField.Unrecognized(unrecognizedValue) =>
              warnAndIgnore(
                s"Ignoring unsupported search term $unrecognizedValue, with values: ${searchTerm.values.mkString("[", ",", "]")}",
              )
        .map(_.collect { case Some(value) => value }),
    )

  private def searchTermFrom[T: Filterable: Put](
      searchTerm: StockRequest.Filter.SearchTerm,
      mapper: String => Option[T],
      name: String,
  ): IO[In[T]] =
    searchTerm.values
      .traverse: value =>
        mapper(value) match
          case Some(category) => IO.some(category)
          case None => warnAndIgnore(s"Ignoring unsupported $name: $value")
      .map: values =>
        In(values.collect { case Some(value) => value }*)

  private def quantityRangeFrom(filterRequest: StockRequest.Filter): Stream[IO, Seq[Filter]] =
    Stream.eval(for
      maybeQuantityRange <- filterRequest.quantityRange
        .flatTraverse: quantityRange =>
          (
            StockAvailability.Quantity.option(quantityRange.min),
            StockAvailability.Quantity.option(quantityRange.max),
          ).tupled match
            case Some((minQuantity, maxQuantity)) =>
              IO.some(Between(Range(minQuantity, maxQuantity)))
            case None =>
              warnAndIgnore[Filter](
                s"Ignoring unsupported quantity range: ${quantityRange.min}:${quantityRange.max}",
              )
      quantityRange = maybeQuantityRange.map(List(_)).getOrElse(List.empty)
    yield quantityRange)

  private def sortFrom(stockRequest: StockRequest): Stream[IO, Sort] =
    Stream.eval(for
      maybeSort <- stockRequest.sort
        .flatTraverse: sortRequest =>
          (sortRequest.field match
            case SortField.Category => OptionT.some[IO](sortableCategory)
            case SortField.Quantity => OptionT.some[IO](sortableQuantity)
            case SortField.Unrecognized(unrecognizedValue) =>
              OptionT(warnAndIgnore(s"Ignoring unsupported sort field: $unrecognizedValue"))
          ).flatMap: sortable =>
            sortRequest.order match
              case SortOrder.Ascending => OptionT.some(Sort.Ascending(sortable))
              case SortOrder.Descending => OptionT.some(Sort.Descending(sortable))
              case SortOrder.Unrecognized(unrecognizedValue) =>
                OptionT(warnAndIgnore(s"Ignoring unsupported sort order: $unrecognizedValue"))
          .value
      sort = maybeSort.getOrElse(NoSort)
    yield sort)

  private def warnAndIgnore[A](message: String) = logger.warn(message) *> IO.none[A]

object StockService:
  def resource(stockRepository: StockRepository, chunkSize: Int)(using
      logger: Logger[IO],
  ): Resource[IO, ServerServiceDefinition] =
    StockFs2Grpc.bindServiceResource[IO](StockService(stockRepository, chunkSize))
