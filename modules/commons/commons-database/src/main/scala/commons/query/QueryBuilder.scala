package es.eriktorr
package commons.query

import cats.data.NonEmptyList
import doobie.Fragment
import doobie.util.fragments.{comma, whereAndOpt}

object QueryBuilder:
  def columns[T <: Column[_]](columns: List[T]): Fragment =
    NonEmptyList.fromList(columns) match
      case Some(xs) => comma(xs.map(x => Fragment.const(x.column)))
      case None => Fragment.empty

  def where(filter: Filter): Fragment =
    val fragments = if filter != Filter.NoFilter then List(filter.toSql) else List.empty
    whereAndOpt(fragments)

  def orderBy(sort: Sort): Fragment = sort.toSql
