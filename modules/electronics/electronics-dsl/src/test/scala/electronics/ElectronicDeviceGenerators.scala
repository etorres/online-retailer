package es.eriktorr
package electronics

import commons.market.EuroMoneyContext
import commons.market.EuroMoneyContext.given
import commons.spec.CollectionGenerators.nDistinct
import commons.spec.StringGenerators.alphaNumericStringBetween

import es.eriktorr.commons.spec.TemporalGenerators.localDateGen
import org.scalacheck.Gen
import squants.energy.{Power, Watts}
import squants.market.Money

import java.time.LocalDate

object ElectronicDeviceGenerators:
  val idGen: Gen[ElectronicDevice.Id] =
    Gen.choose(1L, Long.MaxValue).map(ElectronicDevice.Id.applyUnsafe)

  val categoryGen: Gen[Category] = Gen.oneOf(Category.values.toList)

  val modelGen: Gen[ElectronicDevice.Model] =
    alphaNumericStringBetween(3, 5).map(ElectronicDevice.Model.applyUnsafe)

  val powerConsumptionGen: Gen[Power] = Gen.choose(1d, 3_000d).map(Watts.apply)

  val priceGen: Gen[Money] = (for
    amount <- Gen.choose(1d, 9_000d)
    currency <- Gen.oneOf(euroContext.currencies)
  yield Money(amount, currency))
    .retryUntil(_.in(euroContext.defaultCurrency) <= EuroMoneyContext.max)

  val taxGen: Gen[ElectronicDevice.Tax] = Gen.choose(0d, 1d).map(ElectronicDevice.Tax.applyUnsafe)

  val descriptionGen: Gen[ElectronicDevice.Description] =
    alphaNumericStringBetween(5, 7).map(ElectronicDevice.Description.applyUnsafe)

  val launchDateGen: Gen[LocalDate] = localDateGen

  val imageGen: Gen[ElectronicDevice.Image] = for
    filename <- alphaNumericStringBetween(3, 5)
    extension <- Gen.oneOf("jpg", "png")
  yield ElectronicDevice.Image.applyUnsafe(s"$filename.$extension")

  val imagesGen: Gen[List[ElectronicDevice.Image]] = for
    size <- Gen.choose(1, 3)
    images <- nDistinct(size, imageGen)
  yield images

  def electronicDeviceGen(
      idGen: Gen[ElectronicDevice.Id] = idGen,
      categoryGen: Gen[Category] = categoryGen,
      modelGen: Gen[ElectronicDevice.Model] = modelGen,
      powerConsumptionGen: Gen[Power] = powerConsumptionGen,
      priceGen: Gen[Money] = priceGen,
      taxGen: Gen[ElectronicDevice.Tax] = taxGen,
      descriptionGen: Gen[ElectronicDevice.Description] = descriptionGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
      imagesGen: Gen[List[ElectronicDevice.Image]] = imagesGen,
  ): Gen[ElectronicDevice] = for
    id <- idGen
    category <- categoryGen
    model <- modelGen
    powerConsumption <- powerConsumptionGen
    price <- priceGen
    tax <- taxGen
    description <- descriptionGen
    launchDate <- launchDateGen
    images <- imagesGen
  yield ElectronicDevice(
    id,
    category,
    model,
    powerConsumption,
    price,
    tax,
    description,
    launchDate,
    images,
  )
