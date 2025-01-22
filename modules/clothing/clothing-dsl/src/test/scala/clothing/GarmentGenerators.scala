package es.eriktorr
package clothing

import commons.market.EuroMoneyContext
import commons.market.EuroMoneyContext.euroContext
import commons.spec.CollectionGenerators.nDistinct
import commons.spec.StringGenerators.alphaNumericStringBetween

import org.scalacheck.Gen
import squants.market.Money

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

  val descriptionGen: Gen[Garment.Description] =
    alphaNumericStringBetween(5, 7).map(Garment.Description.applyUnsafe)

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
      descriptionGen: Gen[Garment.Description] = descriptionGen,
      imagesGen: Gen[List[Garment.Image]] = imagesGen,
  ): Gen[Garment] = for
    id <- idGen
    category <- categoryGen
    model <- modelGen
    size <- sizeGen
    color <- colorGen
    price <- priceGen
    description <- descriptionGen
    images <- imagesGen
  yield Garment(id, category, model, size, color, price, description, images)
