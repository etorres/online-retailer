package es.eriktorr
package stock

import commons.api.GrpcServer
import commons.db.JdbcTransactor
import stock.application.{StockServiceConfig, StockServiceParams}

import cats.effect.{ExitCode, IO}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object StockServiceApp extends CommandIOApp(name = "stock-service", header = "Online Retailer"):
  override def main: Opts[IO[ExitCode]] =
    (StockServiceConfig.opts, StockServiceParams.opts).mapN { case (config, _) =>
      program(config)
    }

  private def program(config: StockServiceConfig) = for
    logger <- Slf4jLogger.create[IO]
    given SelfAwareStructuredLogger[IO] = logger
    _ <- logger.info(show"Starting Stock Service with configuration: $config")
    _ <- (for
      transactor <- JdbcTransactor(config.jdbcConfig).resource
      stockRepository = StockRepository.Postgres(transactor)
      stockService <- StockService.resource(stockRepository, chunkSize = 512)
    yield stockService).use: stockService =>
      GrpcServer
        .runServer(stockService, config.grpcConfig)
        .evalMap(server => IO(server.start()))
        .useForever
  yield ExitCode.Success
