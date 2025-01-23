package es.eriktorr
package commons.query

import commons.query.Column.Sortable

import doobie.Fragment
import doobie.implicits.*
import doobie.util.fragments.orderBy

sealed trait Sort:
  def toSql: Fragment

object Sort:
  final case class Ascending[T](sortable: Sortable[T]) extends Sort:
    override def toSql: Fragment = orderByFrom(sortable) ++ fr"ASC"

  final case class Descending[T](sortable: Sortable[T]) extends Sort:
    override def toSql: Fragment = orderByFrom(sortable) ++ fr"DESC"

  private def orderByFrom[T](sortable: Sortable[T]) = orderBy(Fragment.const(sortable.column))

  case object NoSort extends Sort:
    override def toSql: Fragment = Fragment.empty
