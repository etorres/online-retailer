package es.eriktorr
package electronics

import commons.api.TypeExtensions.{toLocalDateOrEpoch, toTimestampOption}
import commons.api.Wire

import io.github.arainko.ducktape.*

trait ProtobufWires:
  given Wire[ElectronicDevice, protobuf.ElectronicDevice] =
    new Wire[ElectronicDevice, protobuf.ElectronicDevice]:
      override def wire(electronicDevice: ElectronicDevice): protobuf.ElectronicDevice =
        electronicDevice
          .into[protobuf.ElectronicDevice]
          .transform(
            Field.computed(_.sku, _.sku),
            Field.computed(_.category, _.category.toString),
            Field.computed(_.launchDate, _.launchDate.toTimestampOption),
            Field.default(_.unknownFields),
          )

      override def unWire(protobufElectronicDevice: protobuf.ElectronicDevice): ElectronicDevice =
        protobufElectronicDevice
          .into[ElectronicDevice]
          .transform(
            Field.computed(_.id, x => ElectronicDevice.Id.applyUnsafe(x.sku)),
            Field.computed(_.category, x => Category.valueOf(x.category)),
            Field.computed(_.model, x => ElectronicDevice.Model.applyUnsafe(x.model)),
            Field.computed(_.tax, x => ElectronicDevice.Tax.applyUnsafe(x.tax)),
            Field.computed(
              _.description,
              x => ElectronicDevice.Description.applyUnsafe(x.description),
            ),
            Field.computed(_.launchDate, _.launchDate.toLocalDateOrEpoch),
          )

object ProtobufWires extends ProtobufWires
