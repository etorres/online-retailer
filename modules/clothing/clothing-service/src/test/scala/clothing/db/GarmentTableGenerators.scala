package es.eriktorr
package clothing.db

import clothing.GarmentGenerators.*
import clothing.{Category, Color, Garment, Size}
import clothing.db.GarmentTable.GarmentDbIn
import commons.market.EuroMoneyContext.given
import taxes.SalesTax
import taxes.TaxGenerators.salesTaxGen

import org.scalacheck.Gen
import squants.market.Money

import java.time.LocalDate

object GarmentTableGenerators:
  def garmentDbInGen(
      idGen: Gen[Garment.Id] = idGen,
      categoryGen: Gen[Category] = categoryGen,
      modelGen: Gen[Garment.Model] = modelGen,
      sizeGen: Gen[Size] = sizeGen,
      colorGen: Gen[Color] = colorGen,
      priceGen: Gen[Money] = priceGen,
      taxGen: Gen[SalesTax] = salesTaxGen,
      descriptionGen: Gen[Garment.Description] = descriptionGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
      imagesGen: Gen[List[Garment.Image]] = imagesGen,
  ): Gen[GarmentDbIn] = for
    garment <- garmentGen(
      idGen = idGen,
      categoryGen = categoryGen,
      modelGen = modelGen,
      sizeGen = sizeGen,
      colorGen = colorGen,
      priceGen = priceGen,
      descriptionGen = descriptionGen,
      launchDateGen = launchDateGen,
      imagesGen = imagesGen,
    )
    tax <- taxGen
  yield GarmentDbIn(
    garment.id,
    garment.category,
    garment.model,
    garment.size,
    garment.color,
    garment.price.in(euroContext.defaultCurrency),
    tax,
    garment.description,
    garment.launchDate,
    garment.images,
  )
