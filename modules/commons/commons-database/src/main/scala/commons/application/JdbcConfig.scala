package es.eriktorr
package commons.application

import commons.Secret
import commons.refined.Constraints.{JdbcUrl, NonEmptyString}

import cats.Show
import cats.collections.Range
import io.github.iltotore.iron.*

final case class JdbcConfig(
    connections: Range[Int],
    connectUrl: JdbcConfig.ConnectUrl,
    driverClassName: JdbcConfig.DriverClassName,
    password: Secret[JdbcConfig.Password],
    username: JdbcConfig.Username,
)

object JdbcConfig:
  opaque type ConnectUrl <: String :| JdbcUrl = String :| JdbcUrl

  object ConnectUrl extends RefinedTypeOps[String, JdbcUrl, ConnectUrl]

  opaque type DriverClassName <: String :| NonEmptyString = String :| NonEmptyString

  object DriverClassName extends RefinedTypeOps[String, NonEmptyString, DriverClassName]

  opaque type Password <: String :| NonEmptyString = String :| NonEmptyString

  object Password extends RefinedTypeOps[String, NonEmptyString, Password]:
    given Show[Password] = Show.fromToString

  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString

  object Username extends RefinedTypeOps[String, NonEmptyString, Username]

  def postgresql(
      connections: Range[Int],
      connectUrl: ConnectUrl,
      password: Secret[Password],
      username: Username,
  ): JdbcConfig = JdbcConfig(
    connections,
    connectUrl,
    DriverClassName.applyUnsafe("org.postgresql.Driver"),
    password,
    username,
  )

  given Show[JdbcConfig] =
    import scala.language.unsafeNulls
    Show.show(config =>
      s"""jdbc-connections: ${config.connections.start}-${config.connections.end},
         | jdbc-connect-url: ${config.connectUrl},
         | jdbc-driver-class-name: ${config.driverClassName},
         | jdbc-password: ${config.password},
         | jdbc-username: ${config.username}""".stripMargin.replaceAll("\\R", ""),
    )
