package es.eriktorr
package commons.query

import commons.query.Column.{ColumnFormatter, Filterable}

import cats.collections.Range
import cats.data.NonEmptySeq
import doobie.implicits.*
import doobie.util.fragment.Fragment.const
import doobie.util.fragments.{and, in, or}
import doobie.{Fragment, Put}

sealed trait Filter:
  def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment

object Filter:
  sealed trait Combinator extends Filter

  object Combinator:
    final case class And(filters: Filter*) extends Combinator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        NonEmptySeq.fromSeq(filters) match
          case Some(xs) => and(xs.map(_.sql))
          case None => Fragment.empty

    final case class In[T: Put](values: T*)(using filterable: Filterable[T]) extends Combinator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        NonEmptySeq.fromSeq(values) match
          case Some(xs) => in(const(formatter.format(filterable)), xs)
          case None => Fragment.empty

    final case class Or(filters: Filter*) extends Combinator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        NonEmptySeq.fromSeq(filters) match
          case Some(xs) => or(xs.map(_.sql))
          case None => Fragment.empty

    final case class Not(arg: Comparator) extends Combinator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        fr"NOT" ++ arg.sql

  sealed trait Comparator extends Filter

  object Comparator:
    final case class Between[T: Put](range: Range[T])(using filterable: Filterable[T])
        extends Comparator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        const(s"${formatter.format(filterable)} BETWEEN") ++ fr"${range.start} AND ${range.end}"

    final case class Equal[T: Put](to: T)(using filterable: Filterable[T]) extends Comparator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        const(s"${formatter.format(filterable)}=") ++ fr"$to"

    final case class Greater[T: Put](than: T, orEqualTo: Boolean = false)(using
        filterable: Filterable[T],
    ) extends Comparator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        const(s"${formatter.format(filterable)} >${if orEqualTo then "=" else ""}") ++ fr"$than"

    final case class Less[T: Put](than: T, orEqualTo: Boolean = false)(using
        filterable: Filterable[T],
    ) extends Comparator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        const(s"${formatter.format(filterable)} <${if orEqualTo then "=" else ""}") ++ fr"$than"

    final case class Like[T](pattern: String)(using filterable: Filterable[T]) extends Comparator:
      override def sql(using formatter: ColumnFormatter = ColumnFormatter.SimpleName): Fragment =
        const(s"${formatter.format(filterable)} LIKE") ++ fr"$pattern"

  case object NoFilter extends Filter:
    override def sql(using formatter: ColumnFormatter): Fragment = Fragment.empty
