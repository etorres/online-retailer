package es.eriktorr
package electronics.db

import commons.market.EuroMoneyContext.given
import commons.query.Row
import electronics.{db, Category, ElectronicDevice as DomainElectronicDevice}

import io.github.arainko.ducktape.*
import squants.energy.Watts
import squants.{Money, Power}

import java.time.LocalDate

final case class ElectronicDevice(
    id: DomainElectronicDevice.Id,
    category: Category,
    model: DomainElectronicDevice.Model,
    powerConsumptionInWatts: Power,
    priceInEur: Money,
    tax: DomainElectronicDevice.Tax,
    description: DomainElectronicDevice.Description,
    launchDate: LocalDate,
    images: List[String],
)

object ElectronicDevice:
  given Row[DomainElectronicDevice, db.ElectronicDevice] =
    new Row[DomainElectronicDevice, db.ElectronicDevice]:
      override def row(domainElectronicDevice: DomainElectronicDevice): db.ElectronicDevice =
        domainElectronicDevice
          .into[db.ElectronicDevice]
          .transform(
            Field.computed(_.powerConsumptionInWatts, _.powerConsumption.in(Watts)),
            Field.computed(_.priceInEur, _.price.in(euroContext.defaultCurrency)),
          )

      override def unRow(ElectronicDeviceRow: db.ElectronicDevice): DomainElectronicDevice =
        ElectronicDeviceRow
          .into[DomainElectronicDevice]
          .transform(
            Field.computed(_.powerConsumption, _.powerConsumptionInWatts),
            Field.computed(_.price, _.priceInEur),
          )
