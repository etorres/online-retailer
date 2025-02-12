package es.eriktorr
package clothing

import commons.market.EuroMoneyContext
import commons.market.EuroMoneyContext.given
import commons.spec.CollectionGenerators.nDistinct
import commons.spec.StringGenerators.alphaNumericStringBetween
import commons.spec.TemporalGenerators.localDateGen

import org.scalacheck.Gen
import squants.market.Money

import java.time.LocalDate

object GarmentGenerators:
  val idGen: Gen[Garment.Id] = Gen.choose(1L, Long.MaxValue).map(Garment.Id.applyUnsafe)

  val categoryGen: Gen[Category] = Gen.oneOf(Category.values.toList)

  val modelGen: Gen[Garment.Model] = alphaNumericStringBetween(3, 5).map(Garment.Model.applyUnsafe)

  val sizeGen: Gen[Size] = Gen.oneOf(Size.values.toList)

  val colorGen: Gen[Color] = Gen.oneOf(Color.values.toList)

  val priceGen: Gen[Money] = (for
    amount <- Gen.choose(1d, 1000d)
    currency <- Gen.oneOf(euroContext.currencies)
  yield Money(amount, currency))
    .retryUntil(_.in(euroContext.defaultCurrency) <= EuroMoneyContext.max)

  val taxGen: Gen[Garment.Tax] = Gen.choose(0d, 1d).map(Garment.Tax.applyUnsafe)

  val descriptionGen: Gen[Garment.Description] =
    alphaNumericStringBetween(5, 7).map(Garment.Description.applyUnsafe)

  val launchDateGen: Gen[LocalDate] = localDateGen

  val imageGen: Gen[Garment.Image] = for
    filename <- alphaNumericStringBetween(3, 5)
    extension <- Gen.oneOf("jpg", "png")
  yield Garment.Image.applyUnsafe(s"$filename.$extension")

  val imagesGen: Gen[List[Garment.Image]] = for
    size <- Gen.choose(1, 3)
    images <- nDistinct(size, imageGen)
  yield images

  def garmentGen(
      idGen: Gen[Garment.Id] = idGen,
      categoryGen: Gen[Category] = categoryGen,
      modelGen: Gen[Garment.Model] = modelGen,
      sizeGen: Gen[Size] = sizeGen,
      colorGen: Gen[Color] = colorGen,
      priceGen: Gen[Money] = priceGen,
      taxGen: Gen[Garment.Tax] = taxGen,
      descriptionGen: Gen[Garment.Description] = descriptionGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
      imagesGen: Gen[List[Garment.Image]] = imagesGen,
  ): Gen[Garment] = for
    id <- idGen
    category <- categoryGen
    model <- modelGen
    size <- sizeGen
    color <- colorGen
    price <- priceGen
    tax <- taxGen
    description <- descriptionGen
    launchDate <- launchDateGen
    images <- imagesGen
  yield Garment(id, category, model, size, color, price, tax, description, launchDate, images)
