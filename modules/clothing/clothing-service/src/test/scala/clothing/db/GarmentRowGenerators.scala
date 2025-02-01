package es.eriktorr
package clothing.db

import clothing.GarmentGenerators.*
import clothing.db.Garment.given
import clothing.{Category, Color, Garment, Size}
import commons.domain.DomainGenerators.salesTaxGen
import commons.domain.SalesTax
import commons.query.Row.row

import org.scalacheck.Gen
import squants.market.Money

import java.time.LocalDate

object GarmentRowGenerators:
  def garmentRowGen(
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
  ): Gen[GarmentRow] = for
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
    garmentRow = garment.row
    tax <- taxGen
  yield GarmentRow(
    garmentRow.id,
    garmentRow.category,
    garmentRow.model,
    garmentRow.size,
    garmentRow.color,
    garmentRow.priceInEur,
    tax,
    garmentRow.description,
    garmentRow.launchDate,
    garmentRow.images,
  )
