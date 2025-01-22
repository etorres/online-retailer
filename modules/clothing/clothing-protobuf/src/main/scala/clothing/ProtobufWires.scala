package es.eriktorr
package clothing

import clothing.Garment.Image
import commons.api.Wire

import io.github.arainko.ducktape.*

trait ProtobufWires:
  given Wire[Garment, protobuf.Garment] = new Wire[Garment, protobuf.Garment]:
    override def wire(garment: Garment): protobuf.Garment =
      garment
        .into[protobuf.Garment]
        .transform(
          Field.computed(_.sku, _.sku),
          Field.computed(_.size, x => s"T_${x.size}"),
          Field.computed(_.category, _.category.toString),
          Field.computed(_.color, _.color.toString),
          Field.default(_.unknownFields),
        )

    override def unWire(protobufGarment: protobuf.Garment): Garment =
      protobufGarment
        .into[Garment]
        .transform(
          Field.computed(_.id, x => Garment.Id.applyUnsafe(x.sku)),
          Field.computed(_.size, x => Size.valueOf(x.size.substring(2).nn)),
          Field.computed(_.color, x => Color.valueOf(x.color)),
          Field.computed(_.category, x => Category.valueOf(x.category)),
          Field.computed(_.model, x => Garment.Model.applyUnsafe(x.model)),
          Field.computed(_.description, x => Garment.Description.applyUnsafe(x.description)),
        )

object ProtobufWires extends ProtobufWires
