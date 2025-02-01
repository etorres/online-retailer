package es.eriktorr
package electronics.db

import commons.domain.DomainGenerators.salesTaxGen
import commons.domain.SalesTax
import commons.query.Row.row
import electronics.db.ElectronicDevice.given
import electronics.ElectronicDeviceGenerators.*
import electronics.{Category, ElectronicDevice}

import org.scalacheck.Gen
import squants.energy.Power
import squants.market.Money

import java.time.LocalDate

object ElectronicDeviceRowGenerators:
  def electronicDeviceRowGen(
      idGen: Gen[ElectronicDevice.Id] = idGen,
      categoryGen: Gen[Category] = categoryGen,
      modelGen: Gen[ElectronicDevice.Model] = modelGen,
      powerConsumptionGen: Gen[Power] = powerConsumptionGen,
      priceGen: Gen[Money] = priceGen,
      taxGen: Gen[SalesTax] = salesTaxGen,
      descriptionGen: Gen[ElectronicDevice.Description] = descriptionGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
      imagesGen: Gen[List[ElectronicDevice.Image]] = imagesGen,
  ): Gen[ElectronicDeviceRow] = for
    electronicDevice <- electronicDeviceGen(
      idGen = idGen,
      categoryGen = categoryGen,
      modelGen = modelGen,
      powerConsumptionGen = powerConsumptionGen,
      priceGen = priceGen,
      descriptionGen = descriptionGen,
      launchDateGen = launchDateGen,
      imagesGen = imagesGen,
    )
    electronicDeviceRow = electronicDevice.row
    tax <- taxGen
  yield ElectronicDeviceRow(
    electronicDevice.id,
    electronicDevice.category,
    electronicDevice.model,
    electronicDevice.powerConsumption,
    electronicDevice.price,
    tax,
    electronicDevice.description,
    electronicDevice.launchDate,
    electronicDevice.images,
  )
