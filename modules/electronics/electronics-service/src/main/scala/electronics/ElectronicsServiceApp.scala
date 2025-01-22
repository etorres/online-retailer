package es.eriktorr
package electronics

import commons.api.GrpcServer
import commons.db.JdbcTransactor
import electronics.application.{ElectronicsServiceConfig, ElectronicsServiceParams}

import cats.effect.{ExitCode, IO}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ElectronicsServiceApp
    extends CommandIOApp(name = "electronics-service", header = "Online Retailer"):
  override def main: Opts[IO[ExitCode]] =
    (ElectronicsServiceConfig.opts, ElectronicsServiceParams.opts).mapN { case (config, _) =>
      program(config)
    }

  private def program(config: ElectronicsServiceConfig) = for
    logger <- Slf4jLogger.create[IO]
    given SelfAwareStructuredLogger[IO] = logger
    _ <- logger.info(show"Starting Electronics Service with configuration: $config")
    _ <- (for
      transactor <- JdbcTransactor(config.jdbcConfig).resource
      electronicsRepository = ElectronicsRepository.Postgres(transactor)
      electronicsService <- ElectronicsService.resource(electronicsRepository, chunkSize = 512)
    yield electronicsService).use: clothingService =>
      GrpcServer
        .runServer(clothingService, config.grpcConfig)
        .evalMap(server => IO(server.start()))
        .useForever
  yield ExitCode.Success
