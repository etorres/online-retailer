package es.eriktorr
package clothing

import cats.Order
import cats.collections.Range

/** Standard sizing.
  * @param range
  *   Numeric sizing.
  */
enum Size(val range: Range[Int]):
  case `4XS` extends Size(Range(0, 0))
  case `3XS` extends Size(Range(2, 4))
  case `2XS` extends Size(Range(6, 8))
  case XS extends Size(Range(10, 12))
  case S extends Size(Range(14, 16))
  case M extends Size(Range(18, 20))
  case L extends Size(Range(22, 24))
  case XL extends Size(Range(26, 28))
  case `2XL` extends Size(Range(30, 32))
  case `3XL` extends Size(Range(34, 36))
  case `4XL` extends Size(Range(38, 40))

object Size:
  def option(value: String): Option[Size] =
    Size.values.find(_.toString.equalsIgnoreCase(value))

  given Order[Size] = Order.by[Size, Int](_.range.start)
