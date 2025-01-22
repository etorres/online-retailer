package es.eriktorr
package clothing.application

import commons.Secret
import commons.application.{GrpcConfig, JdbcConfig}
import commons.spec.TestFilters

import cats.collections.Range
import cats.implicits.catsSyntaxEitherId
import com.comcast.ip4s.{host, port}
import com.monovore.decline.{Command, Help}
import munit.FunSuite

final class ClothingServiceConfigSuite extends FunSuite:
  test("should load configuration from environment"):
    assume(TestFilters.isSbt, "this test runs only on sbt")
    assertEquals(
      Command(name = "name", header = "header")(ClothingServiceConfig.opts)
        .parse(List.empty, sys.env),
      ClothingServiceConfig(
        JdbcConfig.postgresql(
          Range(2, 4),
          JdbcConfig.ConnectUrl.applyUnsafe("jdbc:postgresql://localhost:5432/online_retailer"),
          Secret(JdbcConfig.Password.applyUnsafe("online_retailer_password")),
          JdbcConfig.Username.applyUnsafe("online_retailer_username"),
        ),
        GrpcConfig(host"grpc.test", port"1234"),
      ).asRight[Help],
    )
