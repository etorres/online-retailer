package es.eriktorr
package stock

import commons.market.EuroMoneyContext.given

import scalapb.TypeMapper
import squants.market.Money

import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
given moneyMapper: TypeMapper[String, Money] =
  TypeMapper[String, Money](value =>
    Money(value) match
      case Failure(error) =>
        throw IllegalArgumentException(s"Unsupported money format: $value", error)
      case Success(money) => money,
  )(_.toString)
