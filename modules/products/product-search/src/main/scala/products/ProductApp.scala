package es.eriktorr
package products

import caliban.*
import caliban.quick.*
import es.eriktorr.commons.application.GrpcConfig
import es.eriktorr.products.application.GraphqlConfig
import zio.*

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ProductApp extends ZIOAppDefault:
  override def run: ZIO[Any, Throwable, Unit] =
    for
      graphqlConfig <- ZIO.config[GraphqlConfig]
      _ <- serve(graphqlConfig)
    yield ()

  private def serve(graphqlConfig: GraphqlConfig) =
    ZIO
      .serviceWithZIO[GraphQL[Any]] {
        _.runServer(
          port = graphqlConfig.port.value,
          apiPath = "/api/graphql",
          graphiqlPath = Some("/graphiql"),
        )
      }
      .provide(
        ProductService.make(GrpcConfig(GrpcConfig.defaultHost, GrpcConfig.defaultPort)),
        ProductApi.layer,
      )
