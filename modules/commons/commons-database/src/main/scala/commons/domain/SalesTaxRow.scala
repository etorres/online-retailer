package es.eriktorr
package commons.domain

import commons.refined.Constraints.UnitFraction

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.Interval

final case class SalesTaxRow(id: SalesTaxRow.Id, tax: SalesTax, rate: SalesTaxRow.Rate)

object SalesTaxRow:
  type PositiveShort = Interval.Closed[0, 32_767]

  opaque type Id <: Int :| PositiveShort = Int :| PositiveShort

  object Id extends RefinedTypeOps[Int, PositiveShort, Id]

  opaque type Rate <: Double :| UnitFraction = Double :| UnitFraction

  object Rate extends RefinedTypeOps[Double, UnitFraction, Rate]
