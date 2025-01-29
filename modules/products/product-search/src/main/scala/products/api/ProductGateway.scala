package es.eriktorr
package products.api

import clothing.protobuf.{ClothingRequest, GetGarmentRequest}
import clothing.{ClothingClient, Garment}
import commons.application.GrpcConfig
import commons.market.EuroMoneyContext.given
import electronics.protobuf.ElectronicsRequest
import electronics.{ElectronicDevice, ElectronicsClient}
import products.Product
import products.Product.{Currency, Order, PowerUnit, Range, SearchTerm, Sort}
import stock.protobuf.StockRequest
import stock.{StockAvailability, StockClient}

import cats.Parallel
import cats.effect.{Async, Resource}
import cats.implicits.{catsSyntaxTuple3Parallel, toFunctorOps}
import squants.energy.{BtusPerHour, Milliwatts, Watts}
import squants.market.{EUR, GBP, USD}

import java.time.LocalDate
import scala.collection.immutable.LongMap

trait ProductGateway[F[_]: Async: Parallel]:
  def findProductsBy(
      searchTerms: List[SearchTerm],
      ranges: List[Range],
      maybeSort: Option[Sort],
  ): F[List[Product]]
  def productById(id: Long): F[Option[Product]]

object ProductGateway:
  final class Grpc[F[_]: Async: Parallel](
      clothingClient: ClothingClient[F],
      electronicsClient: ElectronicsClient[F],
      stockClient: StockClient[F],
  ) extends ProductGateway:
    override def findProductsBy(
        searchTerms: List[SearchTerm],
        ranges: List[Range],
        maybeSort: Option[Sort],
    ): F[List[Product]] =
      for
        result <- (
          electronicsClient.findElectronicDevicesBy(
            electronicsRequestFrom(searchTerms, ranges, maybeSort),
          ),
          clothingClient.findGarmentsBy(clothingRequestFrom(searchTerms, ranges, maybeSort)),
          stockClient.findStockAvailabilitiesBy(stockRequestFrom(searchTerms, ranges, maybeSort)),
        ).parTupled
        (electronicDevices, garments, stockAvailabilities) = result
        stockMap = LongMap.from[StockAvailability](
          stockAvailabilities.filter(_.isAvailable).map(x => x.sku -> x),
        )
        electronicDevicesProducts = LongMap
          .from[ElectronicDevice](
            electronicDevices.map(x => x.id -> x),
          )
          .intersectionWith[StockAvailability, Product](
            stockMap,
            (sku, electronicDevice, _) =>
              Product.ElectronicDevice(
                sku,
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
                defaultTax,
                electronicDevice.description,
                LocalDate.now(),
                electronicDevice.images.map(_.value),
              ),
          )
          .values
          .toList
        garmentProducts = LongMap
          .from[Garment](garments.map(x => x.id -> x))
          .intersectionWith[StockAvailability, Product](
            stockMap,
            (sku, garment, _) =>
              Product.Garment(
                sku,
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
                defaultTax,
                garment.description,
                LocalDate.now(),
                garment.images.map(_.value),
              ),
          )
          .values
          .toList
      yield electronicDevicesProducts ++ garmentProducts

    override def productById(id: Long): F[Option[Product]] =
      val maybeGarment = clothingClient.getGarment(GetGarmentRequest(id)).value
      assert(maybeGarment == maybeGarment) // TODO
      ???

    private def electronicsRequestFrom(
        searchTerms: List[SearchTerm],
        ranges: List[Range],
        maybeSort: Option[Sort],
    ) =
      val filterBySearchTerms = searchTerms
        .map { searchTerm =>
          ElectronicsRequest.Filter.SearchTerm.Field.values
            .find(_.name == searchTerm.field)
            .map(field => ElectronicsRequest.Filter.SearchTerm(field, searchTerm.values))
        }
        .collect { case Some(value) => value }
      val priceRange = ranges
        .find(_.field == "PriceRange")
        .map(range =>
          ElectronicsRequest.Filter.PriceRange(
            euroContext.defaultCurrency(range.min),
            euroContext.defaultCurrency(range.max),
          ),
        )
      val powerConsumptionRange = ranges
        .find(_.field == "PowerConsumptionRange")
        .map(range =>
          ElectronicsRequest.Filter.PowerConsumptionRange(
            Watts(range.min),
            Watts(range.max),
          ),
        )
      val filter = ElectronicsRequest.Filter(filterBySearchTerms, priceRange, powerConsumptionRange)
      val sort = maybeSort.flatMap { sort =>
        val maybeField = sort.field match
          case ElectronicsRequest.Sort.Field.Price.name => Some(ElectronicsRequest.Sort.Field.Price)
          case ElectronicsRequest.Sort.Field.PowerConsumption.name =>
            Some(ElectronicsRequest.Sort.Field.PowerConsumption)
          case _ => None
        maybeField.map(field =>
          ElectronicsRequest.Sort(
            field,
            sort.order match
              case Order.Ascending => ElectronicsRequest.Sort.Order.Ascending
              case Order.Descending => ElectronicsRequest.Sort.Order.Descending,
          ),
        )
      }
      ElectronicsRequest(Some(filter), sort)

    private def clothingRequestFrom(
        searchTerms: List[SearchTerm],
        ranges: List[Range],
        maybeSort: Option[Sort],
    ) =
      val filterBySearchTerms = searchTerms
        .map { searchTerm =>
          ClothingRequest.Filter.SearchTerm.Field.values
            .find(_.name == searchTerm.field)
            .map(field => ClothingRequest.Filter.SearchTerm(field, searchTerm.values))
        }
        .collect { case Some(value) => value }
      val priceRange = ranges
        .find(_.field == "PriceRange")
        .map(range =>
          ClothingRequest.Filter.PriceRange(
            euroContext.defaultCurrency(range.min),
            euroContext.defaultCurrency(range.max),
          ),
        )
      val filter = ClothingRequest.Filter(filterBySearchTerms, priceRange)
      val sort = maybeSort.flatMap { sort =>
        val maybeField = sort.field match
          case ClothingRequest.Sort.Field.Price.name => Some(ClothingRequest.Sort.Field.Price)
          case _ => None
        maybeField.map(field =>
          ClothingRequest.Sort(
            field,
            sort.order match
              case Order.Ascending => ClothingRequest.Sort.Order.Ascending
              case Order.Descending => ClothingRequest.Sort.Order.Descending,
          ),
        )
      }
      ClothingRequest(Some(filter), sort)

    private def stockRequestFrom(
        searchTerms: List[SearchTerm],
        ranges: List[Range],
        maybeSort: Option[Sort],
    ) =
      val filterBySearchTerms = searchTerms
        .map { searchTerm =>
          StockRequest.Filter.SearchTerm.Field.values
            .find(_.name == searchTerm.field)
            .map(field => StockRequest.Filter.SearchTerm(field, searchTerm.values))
        }
        .collect { case Some(value) => value }
      val quantityRange = ranges
        .find(_.field == "QuantityRange")
        .map(range => StockRequest.Filter.QuantityRange(range.min.toInt, range.max.toInt))
      val filter = StockRequest.Filter(filterBySearchTerms, quantityRange)
      val sort = maybeSort.flatMap { sort =>
        val maybeField = sort.field match
          case StockRequest.Sort.Field.Category.name => Some(StockRequest.Sort.Field.Category)
          case StockRequest.Sort.Field.Quantity.name => Some(StockRequest.Sort.Field.Quantity)
          case _ => None
        maybeField.map(field =>
          StockRequest.Sort(
            field,
            sort.order match
              case Order.Ascending => StockRequest.Sort.Order.Ascending
              case Order.Descending => StockRequest.Sort.Order.Descending,
          ),
        )
      }
      StockRequest(Some(filter), sort)

  private lazy val defaultTax = 21d

  def resource[F[_]: Async: Parallel](grpcConfig: GrpcConfig): Resource[F, Grpc[F]] =
    for
      clothingClient <- ClothingClient.resource[F](grpcConfig)
      electronicsClient <- ElectronicsClient.resource[F](grpcConfig)
      stockClient <- StockClient.resource[F](grpcConfig)
    yield Grpc(clothingClient, electronicsClient, stockClient)
