package es.eriktorr
package products

import clothing.Garment
import clothing.GarmentGenerators.garmentGen
import commons.market.EuroMoneyContext.given
import commons.spec.TemporalGenerators
import commons.spec.TemporalGenerators.localDateGen
import electronics.ElectronicDevice
import electronics.ElectronicDeviceGenerators.electronicDeviceGen
import products.Product.{Currency, PowerUnit}

import org.scalacheck.Gen
import squants.energy.{BtusPerHour, Milliwatts, Watts}
import squants.market.{EUR, GBP, USD}

import java.time.LocalDate

object ProductGenerators:
  val idGen: Gen[Long] = Gen.choose(1L, Long.MaxValue)

  val launchDateGen: Gen[LocalDate] = localDateGen

  val taxGen: Gen[Double] = Gen.choose(1d, 25d)

  def productGen(
      idGen: Gen[Long] = idGen,
      taxGen: Gen[Double] = taxGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
  ): Gen[Product] = productGenWithSeed(idGen, taxGen, launchDateGen).map:
    case (_, product) => product

  def productGenWithSeed(
      idGen: Gen[Long] = idGen,
      taxGen: Gen[Double] = taxGen,
      launchDateGen: Gen[LocalDate] = launchDateGen,
  ): Gen[(ElectronicDevice | Garment, Product)] = for
    tax <- taxGen
    launchDate <- launchDateGen
    product <- Gen.frequency(
      1 -> electronicDeviceGen(idGen = idGen.map(ElectronicDevice.Id.applyUnsafe)).map(
        electronicDevice =>
          electronicDevice -> Product.ElectronicDevice(
            electronicDevice.id,
            electronicDevice.category.name,
            electronicDevice.model,
            args =>
              (args.powerUnit.getOrElse(PowerUnit.Watts) match
                case PowerUnit.BtusPerHour => electronicDevice.powerConsumption.in(BtusPerHour)
                case PowerUnit.Milliwatts => electronicDevice.powerConsumption.in(Milliwatts)
                case PowerUnit.Watts => electronicDevice.powerConsumption.in(Watts)
              ).value,
            args =>
              (args.currency.getOrElse(Currency.EUR) match
                case Currency.EUR => electronicDevice.price.in(EUR)
                case Currency.GBP => electronicDevice.price.in(GBP)
                case Currency.USD => electronicDevice.price.in(USD)
              ).value,
            tax,
            electronicDevice.description,
            launchDate,
            electronicDevice.images,
          ),
      ),
      1 -> garmentGen(idGen = idGen.map(Garment.Id.applyUnsafe)).map(garment =>
        garment -> Product.Garment(
          garment.id,
          garment.category.name,
          garment.model,
          garment.size.toString,
          garment.color.toString,
          args =>
            (args.currency.getOrElse(Currency.EUR) match
              case Currency.EUR => garment.price.in(EUR)
              case Currency.GBP => garment.price.in(GBP)
              case Currency.USD => garment.price.in(USD)
            ).value,
          tax,
          garment.description,
          launchDate,
          garment.images,
        ),
      ),
    )
  yield product
