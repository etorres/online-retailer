package es.eriktorr
package clothing.db

import clothing.{db, Category, Color, Garment as DomainGarment, Size}
import commons.market.EuroMoneyContext.given
import commons.query.Row

import io.github.arainko.ducktape.*
import squants.Money

final case class Garment(
    id: DomainGarment.Id,
    category: Category,
    model: DomainGarment.Model,
    size: Size,
    color: Color,
    priceInEur: Money,
    description: DomainGarment.Description,
    images: List[String],
)

object Garment:
  given Row[DomainGarment, db.Garment] = new Row[DomainGarment, db.Garment]:
    override def row(domainGarment: DomainGarment): db.Garment =
      domainGarment
        .into[db.Garment]
        .transform(
          Field.computed(_.priceInEur, _.price.in(euroContext.defaultCurrency)),
        )

    override def unRow(garmentRow: db.Garment): DomainGarment =
      garmentRow
        .into[DomainGarment]
        .transform(
          Field.computed(_.price, _.priceInEur),
        )
