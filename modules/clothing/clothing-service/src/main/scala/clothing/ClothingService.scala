package es.eriktorr
package clothing

import clothing.ProtobufWires.given
import clothing.db.GarmentTable.given
import clothing.protobuf.ClothingRequest.Filter.SearchTerm.Field as SearchTermField
import clothing.protobuf.ClothingRequest.Sort.{Field as SortField, Order as SortOrder}
import clothing.protobuf.{
  ClothingFs2Grpc,
  ClothingReply,
  ClothingRequest,
  GetGarmentReply,
  GetGarmentRequest,
}
import commons.api.Wire.wire
import commons.query.Column.Filterable
import commons.query.Filter.Combinator.{And, In}
import commons.query.Filter.Comparator.Between
import commons.query.Filter.NoFilter
import commons.query.Sort.NoSort
import commons.query.{Filter, Sort}

import cats.collections.Range
import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.toTraverseOps
import doobie.Put
import fs2.Stream
import io.github.arainko.ducktape.*
import io.grpc.{Metadata, ServerServiceDefinition}
import org.typelevel.log4cats.Logger

import scala.util.Try

final class ClothingService(clothingRepository: ClothingRepository, chunkSize: Int)(using
    logger: Logger[IO],
) extends ClothingFs2Grpc[IO, Metadata]:
  override def getGarment(
      request: Stream[IO, GetGarmentRequest],
      context: Metadata,
  ): Stream[IO, GetGarmentReply] =
    for
      getGarmentRequest <- request
      maybeGarment <- Stream.eval(
        clothingRepository.findGarmentBy(Garment.Id.applyUnsafe(getGarmentRequest.sku)).value,
      )
    yield GetGarmentReply(maybeGarment.wire)

  override def sendClothingStream(
      request: Stream[IO, ClothingRequest],
      context: Metadata,
  ): Stream[IO, ClothingReply] = request.flatMap: clothingRequest =>
    for
      filter <- filterFrom(clothingRequest)
      sort <- sortFrom(clothingRequest)
      garments <- clothingRepository
        .selectGarmentsBy(filter, sort, chunkSize)
        .chunkMin(chunkSize, allowFewerTotal = true)
        .map(_.toList)
    yield ClothingReply(garments.wire)

  private def filterFrom(clothingRequest: ClothingRequest): Stream[IO, Filter] =
    clothingRequest.filter
      .traverse: filterRequest =>
        for
          searchTerms <- searchTermsFrom(filterRequest)
          priceRange = priceRangeFrom(filterRequest)
        yield And(searchTerms ++ priceRange*)
      .map(_.getOrElse(NoFilter))

  private def searchTermsFrom(filterRequest: ClothingRequest.Filter): Stream[IO, Seq[Filter]] =
    Stream.eval(
      filterRequest.searchTerms
        .traverse: searchTerm =>
          searchTerm.field match
            case SearchTermField.Category => searchTermFrom(searchTerm, Category.option, "category")
            case SearchTermField.Model =>
              searchTermFrom(searchTerm, Garment.Model.option, "model")
            case SearchTermField.Size =>
              searchTermFrom(searchTerm, x => Try(x.to[Size]).toOption, "size")
            case SearchTermField.Color => searchTermFrom(searchTerm, Color.option, "color")
            case SearchTermField.Unrecognized(unrecognizedValue) =>
              warnAndIgnore(
                s"Ignoring unsupported search term $unrecognizedValue, with values: ${searchTerm.values.mkString("[", ",", "]")}",
              )
        .map(_.collect { case Some(value) => value }),
    )

  private def searchTermFrom[T: Filterable: Put](
      searchTerm: ClothingRequest.Filter.SearchTerm,
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

  private def sortFrom(clothingRequest: ClothingRequest): Stream[IO, Sort] =
    Stream.eval(for
      maybeSort <- clothingRequest.sort
        .flatTraverse: sortRequest =>
          (sortRequest.field match
            case SortField.Price => OptionT.some[IO](sortablePrice)
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

  private def priceRangeFrom(filterRequest: ClothingRequest.Filter): List[Filter] =
    filterRequest.priceRange
      .map: priceRange =>
        List(Between(Range(priceRange.min, priceRange.max)))
      .getOrElse(List.empty)

  private def warnAndIgnore[A](message: String) = logger.warn(message) *> IO.none[A]

object ClothingService:
  def resource(clothingRepository: ClothingRepository, chunkSize: Int)(using
      logger: Logger[IO],
  ): Resource[IO, ServerServiceDefinition] =
    ClothingFs2Grpc.bindServiceResource[IO](ClothingService(clothingRepository, chunkSize))
