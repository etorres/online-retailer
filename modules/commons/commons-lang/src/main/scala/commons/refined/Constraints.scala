package es.eriktorr
package commons.refined

import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.numeric.Interval
import io.github.iltotore.iron.constraint.string.{Blank, EndWith, StartWith}

object Constraints:
  type ImagePath = EndWith[".jpg"] | EndWith[".png"]

  type JdbcUrl = StartWith["jdbc:"]

  type NonEmptyString = Not[Blank]

  type UnitFraction = Interval.Closed[0d, 1d]
