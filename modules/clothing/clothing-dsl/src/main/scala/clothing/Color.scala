package es.eriktorr
package clothing

import cats.Order

enum Color:
  case White
  case Silver
  case Gray
  case Black
  case Red
  case Maroon
  case Yellow
  case Olive
  case Lime
  case Green
  case Aqua
  case Teal
  case Blue
  case Navy
  case Fuchsia
  case Purple

object Color:
  def option(value: String): Option[Color] =
    Color.values.find(_.toString.equalsIgnoreCase(value))

  given Order[Color] = Order.by[Color, String](_.toString)
