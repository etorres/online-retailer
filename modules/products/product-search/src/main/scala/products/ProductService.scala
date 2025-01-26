package es.eriktorr
package products

import commons.application.GrpcConfig
import products.Product.{Range, SearchTerm, Sort}
import stock.StockClient

import zio.interop.catz.*
import zio.{Task, ZIO, ZLayer}

trait ProductService:
  def findProductsBy(
      searchTerms: List[SearchTerm],
      ranges: List[Range],
      sort: Option[Sort],
  ): Task[List[Product]]
  def productById(id: Long): Task[Option[Product]]

object ProductService:
  def make(grpcConfig: GrpcConfig): ZLayer[Any, Throwable, ProductService] =
    (for
      stockClient <- StockClient.resource[zio.Task](grpcConfig).toManagedZIO
      productService = new ProductService:
        override def findProductsBy(
            searchTerms: List[SearchTerm],
            ranges: List[Range],
            sort: Option[Sort],
        ): Task[List[Product]] =
          ZIO.succeed(List.empty)
        override def productById(id: Long): Task[Option[Product]] =
          ZIO.succeed(None)
    yield productService).toLayer

// stockClient.findStockAvailabilitiesBy(???)
