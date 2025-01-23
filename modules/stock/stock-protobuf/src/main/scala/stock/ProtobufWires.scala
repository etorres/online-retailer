package es.eriktorr
package stock

import commons.api.Wire

import io.github.arainko.ducktape.*

trait ProtobufWires:
  given Wire[StockAvailability, protobuf.StockAvailability] =
    new Wire[StockAvailability, protobuf.StockAvailability]:
      override def wire(StockAvailability: StockAvailability): protobuf.StockAvailability =
        StockAvailability
          .into[protobuf.StockAvailability]
          .transform(
            Field.computed(_.sku, _.sku),
            Field.default(_.unknownFields),
          )

      override def unWire(
          protobufStockAvailability: protobuf.StockAvailability,
      ): StockAvailability =
        protobufStockAvailability
          .into[StockAvailability]
          .transform(
            Field.computed(_.sku, x => StockAvailability.SKU.applyUnsafe(x.sku)),
            Field.computed(_.name, x => StockAvailability.Name.applyUnsafe(x.name)),
            Field
              .computed(
                _.category,
                x => StockAvailability.Category.applyUnsafe(x.category),
              ),
            Field.computed(_.quantity, x => StockAvailability.Quantity.applyUnsafe(x.quantity)),
            Field.computed(
              _.reorderLevel,
              x => StockAvailability.ReorderLevel.applyUnsafe(x.reorderLevel),
            ),
          )

object ProtobufWires extends ProtobufWires
