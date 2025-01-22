package es.eriktorr
package electronics

import commons.market.EuroMoneyContext.given

import scalapb.TypeMapper
import squants.energy.Power
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

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
given powerMapper: TypeMapper[String, Power] =
  TypeMapper[String, Power](value =>
    Power(value) match
      case Failure(error) =>
        throw IllegalArgumentException(s"Unsupported money format: $value", error)
      case Success(power) => power,
  )(_.toString)
