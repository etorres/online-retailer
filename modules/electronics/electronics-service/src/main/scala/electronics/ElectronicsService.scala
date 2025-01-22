package es.eriktorr
package electronics

import commons.api.Wire.wire
import commons.query.Column.Filterable
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.NoSort
import commons.query.{Filter, Sort}
import electronics.ProtobufWires.given
import electronics.db.ElectronicDeviceConnection.given
import electronics.protobuf.ElectronicsRequest.Filter.SearchTerm.Field as SearchTermField
import electronics.protobuf.ElectronicsRequest.Sort.{Field as SortField, Order as SortOrder}
import electronics.protobuf.{ElectronicsFs2Grpc, ElectronicsReply, ElectronicsRequest}

import cats.collections.Range
import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.toTraverseOps
import doobie.Put
import fs2.Stream
import io.grpc.{Metadata, ServerServiceDefinition}
import org.typelevel.log4cats.Logger

final class ElectronicsService(electronicsRepository: ElectronicsRepository, chunkSize: Int)(using
    logger: Logger[IO],
) extends ElectronicsFs2Grpc[IO, Metadata]:
  override def sendElectronicsStream(
      request: Stream[IO, ElectronicsRequest],
      context: Metadata,
  ): Stream[IO, ElectronicsReply] = request.flatMap: electronicsRequest =>
    for
      filter <- filterFrom(electronicsRequest)
      sort <- sortFrom(electronicsRequest)
      electronicDevices <- electronicsRepository
        .selectElectronicDevicesBy(filter, sort, chunkSize)
        .chunkMin(chunkSize, allowFewerTotal = true)
        .map(_.toList)
    yield ElectronicsReply(electronicDevices.wire)

  private def filterFrom(electronicsRequest: ElectronicsRequest): Stream[IO, Filter] =
    electronicsRequest.filter
      .traverse: filterRequest =>
        for
          searchTerms <- searchTermsFrom(filterRequest)
          powerConsumptionRange = powerConsumptionRangeFrom(filterRequest)
          priceRange = priceRangeFrom(filterRequest)
        yield And(searchTerms ++ powerConsumptionRange ++ priceRange*)
      .map(_.getOrElse(NoFilter))

  private def searchTermsFrom(filterRequest: ElectronicsRequest.Filter): Stream[IO, Seq[Filter]] =
    Stream.eval(
      filterRequest.searchTerms
        .traverse: searchTerm =>
          searchTerm.field match
            case SearchTermField.Category => searchTermFrom(searchTerm, Category.option, "category")
            case SearchTermField.Model =>
              searchTermFrom(searchTerm, ElectronicDevice.Model.option, "model")
            case SearchTermField.Unrecognized(unrecognizedValue) =>
              warnAndIgnore(
                s"Ignoring unsupported search term $unrecognizedValue, with values: ${searchTerm.values.mkString("[", ",", "]")}",
              )
        .map(_.collect { case Some(value) => value }),
    )

  private def searchTermFrom[T: Filterable: Put](
      searchTerm: ElectronicsRequest.Filter.SearchTerm,
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

  private def powerConsumptionRangeFrom(filterRequest: ElectronicsRequest.Filter): List[Filter] =
    filterRequest.powerConsumptionRange
      .map: powerConsumptionRange =>
        List(Between(Range(powerConsumptionRange.min, powerConsumptionRange.max)))
      .getOrElse(List.empty)

  private def priceRangeFrom(filterRequest: ElectronicsRequest.Filter): List[Filter] =
    filterRequest.priceRange
      .map: priceRange =>
        List(Between(Range(priceRange.min, priceRange.max)))
      .getOrElse(List.empty)

  private def sortFrom(electronicsRequest: ElectronicsRequest): Stream[IO, Sort] =
    Stream.eval(for
      maybeSort <- electronicsRequest.sort
        .flatTraverse: sortRequest =>
          (sortRequest.field match
            case SortField.Price => OptionT.some[IO](sortablePrice)
            case SortField.PowerConsumption => OptionT.some[IO](sortablePower)
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

object ElectronicsService:
  def resource(electronicsRepository: ElectronicsRepository, chunkSize: Int)(using
      logger: Logger[IO],
  ): Resource[IO, ServerServiceDefinition] =
    ElectronicsFs2Grpc.bindServiceResource[IO](ElectronicsService(electronicsRepository, chunkSize))
