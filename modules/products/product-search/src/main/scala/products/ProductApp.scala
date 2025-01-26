package es.eriktorr
package products

import caliban.*
import caliban.quick.*
import es.eriktorr.commons.application.GrpcConfig
import zio.*

object ProductApp extends ZIOAppDefault:
  private val serve =
    ZIO
      .serviceWithZIO[GraphQL[Any]] {
        _.runServer(
          port = 8090,
          apiPath = "/api/graphql",
          graphiqlPath = Some("/graphiql"),
        )
      }
      .provide(
        ProductService.make(GrpcConfig(GrpcConfig.defaultHost, GrpcConfig.defaultPort)),
        ProductApi.layer,
      )

  override def run: ZIO[Any, Throwable, Unit] = serve
