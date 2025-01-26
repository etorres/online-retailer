package es.eriktorr
package stock

import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig
import stock.ProtobufWires.given
import stock.protobuf.{StockFs2Grpc, StockRequest}

import cats.effect.{Async, Resource}
import fs2.Stream
import io.grpc.Metadata

trait StockClient[F[_]: Async]:
  def findStockAvailabilitiesBy(request: StockRequest): F[List[StockAvailability]]

object StockClient:
  final class Grpc[F[_]: Async](stockStub: StockFs2Grpc[F, Metadata]) extends StockClient[F]:
    override def findStockAvailabilitiesBy(request: StockRequest): F[List[StockAvailability]] =
      (for
        response <- stockStub.sendStockStream(
          Stream.emit(request).covary[F],
          Metadata(),
        )
        garments = response.stockAvailabilities.toList.unWire
      yield garments).compile.lastOrError

  def resource[F[_]: Async](grpcConfig: GrpcConfig): Resource[F, Grpc[F]] =
    for
      stockStub <- GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => StockFs2Grpc.stubResource[F](channel))
      stockClient = Grpc(stockStub)
    yield stockClient
