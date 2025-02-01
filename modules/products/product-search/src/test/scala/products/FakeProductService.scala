package es.eriktorr
package products

import zio.{Task, ZIO}

final class FakeProductService(products: List[Product] = List.empty) extends ProductService:
  override def findProductsBy(
      searchTerms: List[Product.SearchTerm],
      ranges: List[Product.Range],
      maybeSort: Option[Product.Sort],
  ): Task[List[Product]] = ZIO.succeed(products)

  override def productById(id: Long): Task[Option[Product]] = ZIO.succeed(products.find(_.id == id))
