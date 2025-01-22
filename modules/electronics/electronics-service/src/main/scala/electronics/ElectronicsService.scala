package es.eriktorr
package electronics

import commons.api.Wire.wire
import commons.query.{Filter, Sort}
import electronics.ProtobufWires.given
import electronics.protobuf.ElectronicsRequest.Sort.{Field as SortField, Order as SortOrder}
import electronics.db.ElectronicDeviceConnection.given
import electronics.protobuf.{ElectronicsFs2Grpc, ElectronicsReply, ElectronicsRequest}

import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.toTraverseOps
import es.eriktorr.commons.query.Sort.NoSort
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
    ???

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
