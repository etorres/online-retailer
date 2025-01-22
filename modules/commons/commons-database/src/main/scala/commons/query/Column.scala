package es.eriktorr
package commons.query

import com.softwaremill.tagging.*

final class Column[T](val column: String)

object Column extends AnyTypeclassTaggingCompat:
  type FilterableTag
  type SortableTag

  type Filterable[A] = Column[A] @@ FilterableTag
  type Sortable[A] = Column[A] @@ SortableTag
  type FilterableAndSortable[A] = Column[A] @@ FilterableTag @@ SortableTag

  def column[A](name: String): Column[A] = Column[A](name)

  def filterable[A](name: String): Filterable[A] = Column[A](name).taggedWith[FilterableTag]

  def sortable[A](name: String): Sortable[A] = Column[A](name).taggedWith[SortableTag]

  def filterableAndSortable[A](name: String): FilterableAndSortable[A] =
    Column[A](name).taggedWith[FilterableTag].taggedWith[SortableTag]
