package es.eriktorr
package products

import commons.application.GrpcConfig
import products.Product.{Range, SearchTerm, Sort}
import products.api.ProductGateway

import zio.interop.catz.*
import zio.{Task, ZLayer}

trait ProductService:
  def findProductsBy(
      searchTerms: List[SearchTerm],
      ranges: List[Range],
      maybeSort: Option[Sort],
  ): Task[List[Product]]
  def productById(id: Long): Task[Option[Product]]

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ProductService:
  def make(grpcConfig: GrpcConfig): ZLayer[Any, Throwable, ProductService] =
    (for
      productGateway <- ProductGateway.resource[zio.Task](grpcConfig).toManagedZIO
      productService = new ProductService:
        override def findProductsBy(
            searchTerms: List[SearchTerm],
            ranges: List[Range],
            maybeSort: Option[Sort],
        ): Task[List[Product]] = productGateway.findProductsBy(searchTerms, ranges, maybeSort)
        override def productById(id: Long): Task[Option[Product]] = productGateway.productById(id)
    yield productService).toLayer
