package es.eriktorr
package commons.spec

import cats.collections.Range
import org.scalacheck.Gen
import org.scalacheck.Gen.Choose

object RangeGenerators:
  def rangeIntGen(min: Int = 0, max: Int = Int.MaxValue): Gen[Range[Int]] = rangeGen[Int](min, max)

  def rangeLongGen(min: Long = 0L, max: Long = Long.MaxValue): Gen[Range[Long]] =
    rangeGen[Long](min, max)

  def rangeDoubleGen(min: Double = 0d, max: Double = Double.MaxValue): Gen[Range[Double]] =
    rangeGen[Double](min, max)

  private def rangeGen[T: Choose](min: T, max: T)(using num: Numeric[T]): Gen[Range[T]] =
    for
      x <- Gen.choose(min, max)
      y <- Gen.choose(min, max).retryUntil(_ != x)
    yield Range(num.min(x, y), num.max(x, y))
