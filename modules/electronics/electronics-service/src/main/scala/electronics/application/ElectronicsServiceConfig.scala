package es.eriktorr
package electronics.application

import commons.application.{GrpcConfig, GrpcConfigOpts, JdbcConfig, JdbcConfigOpts}

import cats.Show
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts

final case class ElectronicsServiceConfig(jdbcConfig: JdbcConfig, grpcConfig: GrpcConfig)

object ElectronicsServiceConfig:
  def opts: Opts[ElectronicsServiceConfig] = (
    JdbcConfigOpts.jdbcConfigOpts,
    GrpcConfigOpts.grpcConfigOpts,
  ).mapN(ElectronicsServiceConfig.apply)

  given Show[ElectronicsServiceConfig] =
    import scala.language.unsafeNulls
    Show.show(config => show"""[${config.jdbcConfig},
                              | ${config.grpcConfig}]""".stripMargin.replaceAll("\\R", ""))
