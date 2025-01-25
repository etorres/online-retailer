package es.eriktorr
package stock

import commons.api.GrpcClient
import commons.api.Wire.unWire
import commons.application.GrpcConfig
import stock.ProtobufWires.given
import stock.protobuf.{StockFs2Grpc, StockRequest}

import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.Stream
import io.grpc.Metadata

trait StockClient:
  def findStockAvailabilitiesBy(request: StockRequest): IO[List[StockAvailability]]

object StockClient:
  final class Grpc(grpcConfig: GrpcConfig) extends StockClient:
    override def findStockAvailabilitiesBy(request: StockRequest): IO[List[StockAvailability]] =
      resource.use: service =>
        (for
          response <- service.sendStockStream(
            Stream.emit(request).covary[IO],
            Metadata(),
          )
          garments = response.stockAvailabilities.toList.unWire
        yield garments).compile.lastOrError

    private def resource: Resource[IO, StockFs2Grpc[IO, Metadata]] =
      GrpcClient
        .managedChannelResource(grpcConfig)
        .flatMap(channel => StockFs2Grpc.stubResource[IO](channel))
