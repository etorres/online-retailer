package es.eriktorr
package electronics.db

import commons.domain.{SalesTax, SalesTaxRow}
import electronics.{Category, ElectronicDevice as DomainElectronicDevice}

import io.github.arainko.ducktape.*
import squants.{Money, Power}

import java.time.LocalDate

final case class ElectronicDeviceRow(
    id: DomainElectronicDevice.Id,
    category: Category,
    model: DomainElectronicDevice.Model,
    powerConsumptionInWatts: Power,
    priceInEur: Money,
    tax: SalesTax,
    description: DomainElectronicDevice.Description,
    launchDate: LocalDate,
    images: List[String],
)

object ElectronicDeviceRow:
  def unRowWith(
      electronicDeviceRow: ElectronicDeviceRow,
      salesTaxToRate: Map[SalesTax, SalesTaxRow.Rate],
  ): Option[DomainElectronicDevice] =
    for
      rate <- salesTaxToRate.get(electronicDeviceRow.tax)
      tax <- DomainElectronicDevice.Tax.option(rate)
      electronicDevice = electronicDeviceRow
        .into[DomainElectronicDevice]
        .transform(
          Field.computed(_.powerConsumption, _.powerConsumptionInWatts),
          Field.computed(_.price, _.priceInEur),
          Field.computed(_.tax, _ => DomainElectronicDevice.Tax.applyUnsafe(rate)),
        )
    yield electronicDevice
