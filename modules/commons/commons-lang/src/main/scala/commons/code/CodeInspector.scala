package es.eriktorr
package commons.code

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonInline}
import scala.deriving.Mirror

object CodeInspector:
  inline def fieldNames[A <: Product](using mirror: Mirror.ProductOf[A]): List[String] =
    inline mirror match
      case m: Mirror.ProductOf[A] =>
        summonLabels[m.MirroredElemLabels]

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private inline def summonLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        summonInline[ValueOf[head]].value.asInstanceOf[String] :: summonLabels[tail]
