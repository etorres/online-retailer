package es.eriktorr
package clothing.application

import commons.application.{GrpcConfig, GrpcConfigOpts, JdbcConfig, JdbcConfigOpts}

import cats.Show
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts

final case class ClothingServiceConfig(jdbcConfig: JdbcConfig, grpcConfig: GrpcConfig)

object ClothingServiceConfig:
  def opts: Opts[ClothingServiceConfig] = (
    JdbcConfigOpts.jdbcConfigOpts,
    GrpcConfigOpts.grpcConfigOpts,
  ).mapN(ClothingServiceConfig.apply)

  given Show[ClothingServiceConfig] =
    import scala.language.unsafeNulls
    Show.show(config => show"""[${config.jdbcConfig},
                              | ${config.grpcConfig}]""".stripMargin.replaceAll("\\R", ""))
