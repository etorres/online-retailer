package es.eriktorr
package commons.spec

import cats.collections.Range
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import com.fortysevendeg.scalacheck.datetime.YearRange
import com.fortysevendeg.scalacheck.datetime.instances.jdk8.jdk8LocalDateTime
import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8.arbLocalDateTimeJdk8
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds
import org.scalacheck.Gen

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDate, LocalDateTime}

object TemporalGenerators:
  import scala.language.unsafeNulls

  private given yearRange: YearRange = YearRange.between(1990, 2060)

  def localDateTimeAfter(moment: LocalDateTime): Gen[LocalDateTime] =
    withinLocalDateTimeRange(Range(moment.plusDays(1L), moment.plusYears(1L)))

  def localDateTimeBefore(moment: LocalDateTime): Gen[LocalDateTime] = withinLocalDateTimeRange(
    Range(moment.minusYears(1L), moment.minusDays(1L)),
  )

  val localDateTimeGen: Gen[LocalDateTime] = arbLocalDateTimeJdk8.arbitrary

  val localDateGen: Gen[LocalDate] = localDateTimeGen.map(_.toLocalDate)

  def withinLocalDateTimeRange(localDateTimeRange: Range[LocalDateTime]): Gen[LocalDateTime] =
    genDateTimeWithinRange(
      localDateTimeRange.start,
      Duration.ofDays(ChronoUnit.DAYS.between(localDateTimeRange.start, localDateTimeRange.end)),
    )
