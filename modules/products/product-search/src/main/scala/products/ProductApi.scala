package es.eriktorr
package products

import products.Product.Queries

import caliban.*
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers.*
import caliban.wrappers.{Caching, IncrementalDelivery}
import zio.{durationInt, ZIO, ZLayer}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ProductApi:
  private def makeApi(productService: ProductService) =
    graphQL(
      RootResolver(
        Queries(
          products = args =>
            productService.findProductsBy(
              args.lookup.searchTerms,
              args.lookup.ranges,
              args.lookup.sort,
            ),
          product = args => productService.productById(args.id),
        ),
      ),
    ) @@
      apolloTracing() @@
      maxFields(300) @@
      maxDepth(30) @@
      logSlowQueries(500.millis) @@
      timeout(3.seconds) @@
      Caching.extension() @@
      IncrementalDelivery.defer

  val layer: ZLayer[ProductService, Nothing, GraphQL[Any]] =
    ZLayer(ZIO.serviceWith[ProductService](makeApi))
