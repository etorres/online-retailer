package es.eriktorr
package products

import caliban.schema.Annotations.{GQLDefault, GQLDescription}
import caliban.schema.{ArgBuilder, Schema}
import zio.UIO

import java.time.LocalDate

sealed trait Product derives Schema.SemiAuto:
  val id: Long
  val category: String
  val model: String
  val price: Product.CurrencyArgs => Double
  val tax: Double
  val description: String
  val launchDate: LocalDate
  val images: List[String]

object Product:
  sealed trait Currency derives Schema.SemiAuto, ArgBuilder

  object Currency:
    case object EUR extends Currency derives Schema.SemiAuto, ArgBuilder
    case object GBP extends Currency derives Schema.SemiAuto, ArgBuilder
    case object USD extends Currency derives Schema.SemiAuto, ArgBuilder

  final case class CurrencyArgs(@GQLDefault("EUR") currency: Option[Currency])
      derives Schema.SemiAuto,
        ArgBuilder

  sealed trait PowerUnit derives Schema.SemiAuto, ArgBuilder:
    val code: String

  object PowerUnit:
    case object BtusPerHour extends PowerUnit derives Schema.SemiAuto, ArgBuilder:
      override val code: String = "Btu/hr"
    case object Milliwatts extends PowerUnit derives Schema.SemiAuto, ArgBuilder:
      override val code: String = "mW"
    case object Watts extends PowerUnit derives Schema.SemiAuto, ArgBuilder:
      override val code: String = "W"

  final case class PowerUnitArgs(@GQLDefault("Watts") powerUnit: Option[PowerUnit])
      derives Schema.SemiAuto,
        ArgBuilder

  final case class ElectronicDevice(
      id: Long,
      category: String,
      model: String,
      powerConsumption: PowerUnitArgs => Double,
      price: CurrencyArgs => Double,
      tax: Double,
      description: String,
      launchDate: LocalDate,
      images: List[String],
  ) extends Product derives Schema.SemiAuto

  final case class Garment(
      id: Long,
      category: String,
      model: String,
      size: String,
      color: String,
      price: CurrencyArgs => Double,
      tax: Double,
      description: String,
      launchDate: LocalDate,
      images: List[String],
  ) extends Product derives Schema.SemiAuto

  sealed trait Filter derives Schema.SemiAuto, ArgBuilder

  object Filter:
    final case class Range(field: String, min: Double, max: Double) extends Filter
        derives Schema.SemiAuto,
          ArgBuilder

    final case class SearchTerm(field: String, values: List[String]) extends Filter
        derives Schema.SemiAuto,
          ArgBuilder

  sealed trait Sort derives Schema.SemiAuto, ArgBuilder:
    val field: String

  object Sort:
    final case class Ascending(field: String) extends Sort derives Schema.SemiAuto, ArgBuilder
    final case class Descending(field: String) extends Sort derives Schema.SemiAuto, ArgBuilder

  final case class ProductsArgs(filters: List[Filter], sort: Option[Sort])
      derives Schema.SemiAuto,
        ArgBuilder
  final case class ProductArgs(id: Long) derives Schema.SemiAuto, ArgBuilder

  final case class Queries(
      @GQLDescription("Returns all products that match the given filter")
      products: ProductsArgs => UIO[List[Product]],
      @GQLDescription("Find the product by its id")
      product: ProductArgs => UIO[Option[Product]],
  ) derives Schema.SemiAuto
