package es.eriktorr
package electronics

import commons.refined.Constraints.{ImagePath, NonEmptyString, UnitFraction}

import cats.Order
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import io.github.arainko.ducktape.Transformer
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive0
import io.hypersistence.tsid.TSID
import squants.{Money, Power}

import java.time.LocalDate

final case class ElectronicDevice(
    id: ElectronicDevice.Id,
    category: Category,
    model: ElectronicDevice.Model,
    powerConsumption: Power,
    price: Money,
    tax: ElectronicDevice.Tax,
    description: ElectronicDevice.Description,
    launchDate: LocalDate,
    images: List[ElectronicDevice.Image],
):
  def sku: ElectronicDevice.Id = id

object ElectronicDevice:
  opaque type Id <: Long :| Positive0 = Long :| Positive0

  object Id extends RefinedTypeOps[Long, Positive0, Id]:
    def io(tsid: TSID): IO[Id] =
      IO.fromEither(Id.either(tsid.toLong.nn).leftMap(IllegalArgumentException(_)))

  opaque type Model <: String :| NonEmptyString = String :| NonEmptyString

  object Model extends RefinedTypeOps[String, NonEmptyString, Model]

  opaque type Tax <: Double :| UnitFraction = Double :| UnitFraction

  object Tax extends RefinedTypeOps[Double, UnitFraction, Tax]

  opaque type Description <: String :| NonEmptyString = String :| NonEmptyString

  object Description extends RefinedTypeOps[String, NonEmptyString, Description]

  opaque type Image <: String :| ImagePath = String :| ImagePath

  object Image extends RefinedTypeOps[String, ImagePath, Image]

  given Transformer[String, Image] = (value: String) => Image.applyUnsafe(value)

  given Order[ElectronicDevice] = Order.by[ElectronicDevice, Long](_.id)
