package es.eriktorr
package commons.query

import commons.query.Column.ColumnFormatter

import cats.data.NonEmptyList
import doobie.Fragment
import doobie.implicits.*
import doobie.util.fragment.Fragment.const
import doobie.util.fragments.{comma as doobieComma, whereAndOpt}

object QueryBuilder:
  def comma[T <: Column[_]](columns: List[T])(using
      formatter: ColumnFormatter = ColumnFormatter.SimpleName,
  ): Fragment =
    NonEmptyList.fromList(columns) match
      case Some(columnsNel) =>
        doobieComma(columnsNel.map(column => const(formatter.format(column))))
      case None => Fragment.empty

  def join[A, B](columnA: Column[A], columnB: Column[B]): Fragment =
    fr"FROM" ++ const(columnA.table) ++
      fr"INNER JOIN" ++ const(columnB.table) ++
      fr"ON" ++ const(columnA.fqn) ++ fr"=" ++ const(columnB.fqn)

  def where(filter: Filter)(using
      formatter: ColumnFormatter = ColumnFormatter.SimpleName,
  ): Fragment =
    val fragments = if filter != Filter.NoFilter then List(filter.sql) else List.empty
    whereAndOpt(fragments)

  def orderBy(sort: Sort): Fragment = sort.sql
