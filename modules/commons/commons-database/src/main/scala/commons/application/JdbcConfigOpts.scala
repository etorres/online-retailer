package es.eriktorr
package commons.application

import commons.Secret
import commons.application.RangeArgument.given

import cats.collections.Range
import cats.implicits.catsSyntaxTuple4Semigroupal
import com.monovore.decline.Opts
import io.github.iltotore.iron.decline.given

object JdbcConfigOpts:
  def jdbcConfigOpts: Opts[JdbcConfig] = (
    Opts
      .env[Range[Int]](
        name = "ONLINE_RETAILER_JDBC_CONNECTIONS",
        help = "Set JDBC Connections.",
      )
      .validate("Must be between 1 and 16")(_.overlaps(Range(1, 16)))
      .withDefault(Range(1, 3)),
    Opts.env[JdbcConfig.ConnectUrl](
      name = "ONLINE_RETAILER_JDBC_CONNECT_URL",
      help = "Set JDBC Connect URL.",
    ),
    Opts
      .env[JdbcConfig.Password](
        name = "ONLINE_RETAILER_JDBC_PASSWORD",
        help = "Set JDBC Password.",
      )
      .map(Secret.apply[JdbcConfig.Password]),
    Opts.env[JdbcConfig.Username](
      name = "ONLINE_RETAILER_JDBC_USERNAME",
      help = "Set JDBC Username.",
    ),
  ).mapN(JdbcConfig.postgresql)
