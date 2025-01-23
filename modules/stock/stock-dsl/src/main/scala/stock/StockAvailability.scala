package es.eriktorr
package stock

import commons.refined.Constraints.NonEmptyString

import cats.Order
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.{Positive, Positive0}
import io.hypersistence.tsid.TSID
import squants.Money

final case class StockAvailability(
    sku: StockAvailability.SKU,
    name: StockAvailability.Name,
    category: StockAvailability.Category,
    quantity: StockAvailability.Quantity,
    unitPrice: Money,
    reorderLevel: StockAvailability.ReorderLevel,
)

object StockAvailability:
  opaque type SKU <: Long :| Positive0 = Long :| Positive0

  object SKU extends RefinedTypeOps[Long, Positive0, SKU]:
    def io(tsid: TSID): IO[SKU] =
      IO.fromEither(SKU.either(tsid.toLong.nn).leftMap(IllegalArgumentException(_)))

  opaque type Name <: String :| NonEmptyString = String :| NonEmptyString

  object Name extends RefinedTypeOps[String, NonEmptyString, Name]

  opaque type Category <: String :| NonEmptyString = String :| NonEmptyString

  object Category extends RefinedTypeOps[String, NonEmptyString, Category]

  opaque type Quantity <: Int :| Positive0 = Int :| Positive0

  object Quantity extends RefinedTypeOps[Int, Positive0, Quantity]

  opaque type ReorderLevel <: Int :| Positive = Int :| Positive

  object ReorderLevel extends RefinedTypeOps[Int, Positive, ReorderLevel]

  given Order[StockAvailability] = Order.by[StockAvailability, Long](_.sku)
