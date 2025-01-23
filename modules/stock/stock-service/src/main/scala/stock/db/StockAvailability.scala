package es.eriktorr
package stock.db

import commons.market.EuroMoneyContext.given
import commons.query.Row
import stock.{db, StockAvailability as DomainStockAvailability}

import io.github.arainko.ducktape.*
import squants.Money

final case class StockAvailability(
    sku: DomainStockAvailability.SKU,
    name: DomainStockAvailability.Name,
    category: DomainStockAvailability.Category,
    quantity: DomainStockAvailability.Quantity,
    unitPriceInEur: Money,
    reorderLevel: DomainStockAvailability.ReorderLevel,
)

object StockAvailability:
  given Row[DomainStockAvailability, db.StockAvailability] =
    new Row[DomainStockAvailability, db.StockAvailability]:
      override def row(domainStockAvailability: DomainStockAvailability): db.StockAvailability =
        domainStockAvailability
          .into[db.StockAvailability]
          .transform(
            Field.computed(_.unitPriceInEur, _.unitPrice.in(euroContext.defaultCurrency)),
          )

      override def unRow(StockAvailabilityRow: db.StockAvailability): DomainStockAvailability =
        StockAvailabilityRow
          .into[DomainStockAvailability]
          .transform(
            Field.computed(_.unitPrice, _.unitPriceInEur),
          )
