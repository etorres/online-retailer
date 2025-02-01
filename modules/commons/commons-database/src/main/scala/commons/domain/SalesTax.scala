package es.eriktorr
package commons.domain

import cats.implicits.catsSyntaxEitherId
import doobie.Meta
import doobie.postgres.implicits.pgEnumStringOpt

enum SalesTax(val value: String):
  case LuxuryGoods extends SalesTax("luxury goods")
  case StandardRate extends SalesTax("standard rate")
  case ReducedRate extends SalesTax("reduced rate")
  case EssentialGoods extends SalesTax("essential goods")

object SalesTax:
  def option(value: String): Option[SalesTax] =
    SalesTax.values.find(_.value == value)

  def either(value: String): Either[String, SalesTax] =
    SalesTax.values.find(_.value == value) match
      case Some(salesTax) => salesTax.asRight
      case None => s"Unsupported sales tax: $value".asLeft

  given Meta[SalesTax] = pgEnumStringOpt("sales_tax", SalesTax.option, _.value)
