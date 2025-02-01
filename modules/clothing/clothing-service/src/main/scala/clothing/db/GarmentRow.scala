package es.eriktorr
package clothing.db

import clothing.{Category, Color, Garment as DomainGarment, Size}
import commons.domain.{SalesTax, SalesTaxRow}

import io.github.arainko.ducktape.*
import squants.Money

import java.time.LocalDate

final case class GarmentRow(
    id: DomainGarment.Id,
    category: Category,
    model: DomainGarment.Model,
    size: Size,
    color: Color,
    priceInEur: Money,
    tax: SalesTax,
    description: DomainGarment.Description,
    launchDate: LocalDate,
    images: List[String],
)

object GarmentRow:
  def unRowWith(
      garmentRow: GarmentRow,
      salesTaxToRate: Map[SalesTax, SalesTaxRow.Rate],
  ): Option[DomainGarment] =
    for
      rate <- salesTaxToRate.get(garmentRow.tax)
      tax <- DomainGarment.Tax.option(rate)
      garment = garmentRow
        .into[DomainGarment]
        .transform(
          Field.computed(_.price, _.priceInEur),
          Field.computed(_.tax, _ => DomainGarment.Tax.applyUnsafe(rate)),
        )
    yield garment
