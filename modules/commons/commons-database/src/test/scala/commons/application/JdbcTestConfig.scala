package es.eriktorr
package commons.application

import commons.Secret

import cats.collections.Range

enum JdbcTestConfig(val config: JdbcConfig, val database: String):
  case LocalContainer
      extends JdbcTestConfig(
        JdbcConfig.postgresql(
          Range(1, 3),
          JdbcConfig.ConnectUrl
            .applyUnsafe(s"jdbc:postgresql://${JdbcTestConfig.host}/${JdbcTestConfig.database}"),
          Secret(JdbcConfig.Password.applyUnsafe("changeMe")),
          JdbcConfig.Username.applyUnsafe("test"),
        ),
        JdbcTestConfig.database,
      )

object JdbcTestConfig:
  final private val host = "postgres.test:5432"
  final private val database = "online_retailer"
