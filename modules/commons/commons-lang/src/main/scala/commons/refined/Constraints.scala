package es.eriktorr
package commons.refined

import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.numeric.{GreaterEqual, LessEqual}
import io.github.iltotore.iron.constraint.string.{Blank, EndWith, StartWith}

object Constraints:
  type Between[Min, Max] = GreaterEqual[Min] & LessEqual[Max]

  type ImagePath = EndWith[".jpg"] | EndWith[".png"]

  type JdbcUrl = StartWith["jdbc:"]

  type NonEmptyString = Not[Blank]
