package es.eriktorr
package products

import caliban.*
import caliban.quick.*
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
        ProductService.make(???),
        ProductApi.layer,
      )

  override def run: ZIO[Any, Throwable, Unit] = serve

/*
package es.eriktorr
package products

import products.Product.Queries

import caliban.render

object DeleteMe:
  def main(args: Array[String]): Unit =
    println(render[Queries])
*/