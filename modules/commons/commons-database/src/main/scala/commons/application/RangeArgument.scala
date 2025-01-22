package es.eriktorr
package commons.application

import cats.collections.Range
import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import com.monovore.decline.Argument

trait RangeArgument:
  given intRangeArgument: Argument[Range[Int]] = new Argument[Range[Int]]:
    override def read(string: String): ValidatedNel[String, Range[Int]] =
      import scala.language.unsafeNulls
      string.split(":", 2) match
        case Array(minimum, maximum) => Range(minimum.toInt, maximum.toInt).validNel
        case _ => s"Invalid minimum:maximum range: $string".invalidNel
    override def defaultMetavar: String = "minimum:maximum"

object RangeArgument extends RangeArgument
