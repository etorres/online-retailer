package es.eriktorr
package commons.api

trait Wire[A, B <: scalapb.GeneratedMessage] extends Wire.ToWire[A, B] with Wire.FromWire[B, A]

object Wire:
  trait ToWire[A, B <: scalapb.GeneratedMessage]:
    def wire(a: A): B

  trait FromWire[B <: scalapb.GeneratedMessage, A]:
    def unWire(b: B): A

  extension [A](self: List[A])
    def wire[B <: scalapb.GeneratedMessage](using transformer: ToWire[A, B]): List[B] =
      self.map: a =>
        transformer.wire(a)

  extension [B <: scalapb.GeneratedMessage](self: List[B])
    def unWire[A](using transformer: FromWire[B, A]): List[A] =
      self.map: b =>
        transformer.unWire(b)
