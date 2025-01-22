package es.eriktorr
package clothing

import clothing.application.{ClothingServiceConfig, ClothingServiceParams}
import commons.api.GrpcServer
import commons.db.JdbcTransactor
import commons.std.TSIDGen

import cats.effect.{ExitCode, IO}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ClothingServiceApp
    extends CommandIOApp(name = "clothing-service", header = "Online Retailer"):
  override def main: Opts[IO[ExitCode]] =
    (ClothingServiceConfig.opts, ClothingServiceParams.opts).mapN { case (config, _) =>
      program(config)
    }

  private def program(config: ClothingServiceConfig) = for
    logger <- Slf4jLogger.create[IO]
    given SelfAwareStructuredLogger[IO] = logger
    given TSIDGen[IO] = TSIDGen[IO]
    _ <- logger.info(show"Starting Clothing Service with configuration: $config")
    _ <- (for
      transactor <- JdbcTransactor(config.jdbcConfig).resource
      clothingRepository = ClothingRepository.Postgres(transactor)
      clothingService <- ClothingService.resource(clothingRepository)
    yield clothingService).use: clothingService =>
      GrpcServer
        .runServer(clothingService, config.grpcConfig)
        .evalMap(server => IO(server.start()))
        .useForever
  yield ExitCode.Success
