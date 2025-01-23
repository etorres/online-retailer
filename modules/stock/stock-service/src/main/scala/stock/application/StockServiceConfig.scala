package es.eriktorr
package stock.application

import commons.application.{GrpcConfig, GrpcConfigOpts, JdbcConfig, JdbcConfigOpts}

import cats.Show
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts

final case class StockServiceConfig(jdbcConfig: JdbcConfig, grpcConfig: GrpcConfig)

object StockServiceConfig:
  def opts: Opts[StockServiceConfig] = (
    JdbcConfigOpts.jdbcConfigOpts,
    GrpcConfigOpts.grpcConfigOpts,
  ).mapN(StockServiceConfig.apply)

  given Show[StockServiceConfig] =
    import scala.language.unsafeNulls
    Show.show(config => show"""[${config.jdbcConfig},
                              | ${config.grpcConfig}]""".stripMargin.replaceAll("\\R", ""))
