package es.eriktorr
package commons.query

import commons.query.Column.Filterable

import cats.collections.Range
import cats.data.NonEmptySeq
import doobie.implicits.*
import doobie.util.fragments.{and, in, or}
import doobie.{Fragment, Put}

sealed trait Filter:
  def toSql: Fragment

object Filter:
  sealed trait Combinator extends Filter

  object Combinator:
    final case class And(filters: Filter*) extends Combinator:
      override def toSql: Fragment = NonEmptySeq.fromSeq(filters) match
        case Some(xs) => and(xs.map(_.toSql))
        case None => Fragment.empty

    final case class In[T: Put](values: T*)(using filterable: Filterable[T]) extends Combinator:
      override def toSql: Fragment = NonEmptySeq.fromSeq(values) match
        case Some(xs) => in(Fragment.const(filterable.column), xs)
        case None => Fragment.empty

    final case class Or(filters: Filter*) extends Combinator:
      override def toSql: Fragment = NonEmptySeq.fromSeq(filters) match
        case Some(xs) => or(xs.map(_.toSql))
        case None => Fragment.empty

    final case class Not(arg: Comparator) extends Combinator:
      override def toSql: Fragment = fr"NOT" ++ arg.toSql

  sealed trait Comparator extends Filter

  object Comparator:
    final case class Between[T: Put](range: Range[T])(using filterable: Filterable[T])
        extends Comparator:
      override def toSql: Fragment =
        Fragment.const(s"${filterable.column} BETWEEN") ++ fr"${range.start} AND ${range.end}"

    final case class Equal[T: Put](to: T)(using filterable: Filterable[T]) extends Comparator:
      override def toSql: Fragment = Fragment.const(s"${filterable.column}=") ++ fr"$to"

    final case class Greater[T: Put](than: T, orEqualTo: Boolean = false)(using
        filterable: Filterable[T],
    ) extends Comparator:
      override def toSql: Fragment = Fragment.const(
        s"${filterable.column} >${if orEqualTo then "=" else ""}",
      ) ++ fr"$than"

    final case class Less[T: Put](than: T, orEqualTo: Boolean = false)(using
        filterable: Filterable[T],
    ) extends Comparator:
      override def toSql: Fragment = Fragment.const(
        s"${filterable.column} <${if orEqualTo then "=" else ""}",
      ) ++ fr"$than"

    final case class Like[T](pattern: String)(using filterable: Filterable[T]) extends Comparator:
      override def toSql: Fragment = Fragment.const(s"${filterable.column} LIKE") ++ fr"$pattern"

  case object NoFilter extends Filter:
    override def toSql: Fragment = Fragment.empty
