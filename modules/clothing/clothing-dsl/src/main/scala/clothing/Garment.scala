package es.eriktorr
package clothing

import commons.refined.Constraints.{ImagePath, NonEmptyString}

import cats.Order
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import io.github.arainko.ducktape.Transformer
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Positive0
import io.hypersistence.tsid.TSID
import squants.Money

final case class Garment(
    id: Garment.Id,
    category: Category,
    model: Garment.Model,
    size: Size,
    color: Color,
    price: Money,
    description: Garment.Description,
    images: List[Garment.Image],
):
  def sku: Garment.Id = id

object Garment:
  opaque type Id <: Long :| Positive0 = Long :| Positive0

  object Id extends RefinedTypeOps[Long, Positive0, Id]:
    def io(tsid: TSID): IO[Id] =
      IO.fromEither(Id.either(tsid.toLong.nn).leftMap(IllegalArgumentException(_)))

  opaque type Model <: String :| NonEmptyString = String :| NonEmptyString

  object Model extends RefinedTypeOps[String, NonEmptyString, Model]

  opaque type Description <: String :| NonEmptyString = String :| NonEmptyString

  object Description extends RefinedTypeOps[String, NonEmptyString, Description]

  opaque type Image <: String :| ImagePath = String :| ImagePath

  object Image extends RefinedTypeOps[String, ImagePath, Image]

  given Transformer[String, Image] = (value: String) => Image.applyUnsafe(value)

  given Order[Garment] = Order.by[Garment, Long](_.id)
