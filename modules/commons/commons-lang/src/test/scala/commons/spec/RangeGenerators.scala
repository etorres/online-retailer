package es.eriktorr
package commons.spec

import cats.collections.Range
import cats.implicits.catsSyntaxTuple2Semigroupal
import org.scalacheck.Gen
import org.scalacheck.Gen.Choose
import org.scalacheck.cats.implicits.given

object RangeGenerators:
  def rangeGen[T: Choose](min: T, max: T)(using num: Numeric[T]): Gen[Range[T]] = for
    (x, y) <- (Gen.chooseNum(min, max), Gen.chooseNum(min, max)).tupled
    (min, max) = (num.min(x, y), num.max(x, y))
  yield Range(min, max)

  def rangeIntGen(min: Int = 0, max: Int = Int.MaxValue): Gen[Range[Int]] = rangeGen[Int](min, max)

  def rangeLongGen(min: Long = 0L, max: Long = Long.MaxValue): Gen[Range[Long]] =
    rangeGen[Long](min, max)

  def rangeDoubleGen(min: Double = 0d, max: Double = Double.MaxValue): Gen[Range[Double]] =
    rangeGen[Double](min, max)
