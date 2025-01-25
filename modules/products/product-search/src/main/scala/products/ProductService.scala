package es.eriktorr
package products

import products.Product.{Filter, Sort}
import stock.StockClient

import zio.{UIO, ZIO, ZLayer}
//import zio.interop.catz.*

trait ProductService:
  def findProductsBy(filters: List[Filter], sort: Option[Sort]): UIO[List[Product]]
  def productById(id: Long): UIO[Option[Product]]

object ProductService:
  def make(stockClient: StockClient): ZLayer[Any, Nothing, ProductService] =
    ZLayer.succeed(new ProductService:
      override def findProductsBy(filters: List[Filter], sort: Option[Sort]): UIO[List[Product]] =
//        stockClient.findStockAvailabilitiesBy[zio.Task](???)
        ???

      override def productById(id: Long): UIO[Option[Product]] =
        ZIO.succeed(None), // TODO
    )
