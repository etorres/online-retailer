package es.eriktorr
package commons.query

import com.softwaremill.tagging.*

final class Column[A](val table: Table.Name, val column: String):
  val fqn = s"$table.$column"

object Column extends AnyTypeclassTaggingCompat:
  type FilterableTag
  type SortableTag

  type Filterable[A] = Column[A] @@ FilterableTag
  type Sortable[A] = Column[A] @@ SortableTag
  type FilterableAndSortable[A] = Column[A] @@ FilterableTag @@ SortableTag

  enum ColumnFormatter:
    case SimpleName, FullQualifiedName
    def format[A](column: Column[A]): String =
      this match
        case ColumnFormatter.SimpleName => column.column
        case ColumnFormatter.FullQualifiedName => column.fqn

  def column[A](name: String)(using table: Table.Name): Column[A] = Column[A](table, name)

  def filterable[A](name: String)(using table: Table.Name): Filterable[A] =
    Column[A](table, name).taggedWith[FilterableTag]

  def sortable[A](name: String)(using table: Table.Name): Sortable[A] =
    Column[A](table, name).taggedWith[SortableTag]

  def filterableAndSortable[A](name: String)(using table: Table.Name): FilterableAndSortable[A] =
    Column[A](table, name).taggedWith[FilterableTag].taggedWith[SortableTag]
