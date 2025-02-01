package es.eriktorr
package commons.api

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp.Timestamp

import java.time.{Instant, LocalDate, ZoneOffset}

object TypeExtensions:
  extension (self: Timestamp)
    def toLocalDate: LocalDate =
      val instant = Instant.ofEpochSecond(self.seconds, self.nanos)
      instant.atZone(ZoneOffset.UTC).toLocalDate

  extension (self: Option[Timestamp])
    def toLocalDateOrEpoch: LocalDate =
      self.map(_.toLocalDate).getOrElse(LocalDate.EPOCH)

  extension (self: LocalDate)
    def toTimestamp: Timestamp =
      val instant = self.atStartOfDay(ZoneOffset.UTC).toInstant
      Timestamp.of(instant.getEpochSecond, instant.getNano)

    def toTimestampOption: Option[Timestamp] =
      self.toTimestamp.some
