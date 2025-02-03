package es.eriktorr
package commons.query

import commons.query.Column.Filterable
import commons.query.Filter.Comparator.Equal
import commons.query.QueryBuilder.{comma, orderBy, where}

import com.softwaremill.tagging.*
import doobie.implicits.*
import doobie.util.fragments.parentheses
import doobie.{ConnectionIO, Fragment, Put, Read}
import fs2.Stream

trait Table[Id: Put: Filterable, Input, Output: Read]:
  implicit val name: Table.Name

  def sql: Fragment = Fragment.const(name)
  def read: List[Column[_]]
  def write(value: Input): Table.Write

  def insert(item: Input): ConnectionIO[Int] =
    val sql = fr"INSERT INTO" ++ this.sql ++ write(item).sql
    sql.update.run

  def findBy(id: Id): ConnectionIO[Option[Output]] =
    val select = fr"SELECT" ++ comma(read) ++ fr"FROM" ++ this.sql
    val sql = select ++ where(Equal(id))
    sql.query[Output].option

  def selectBy(filter: Filter, sort: Sort, chunkSize: Int): Stream[ConnectionIO, Output] =
    val select = fr"SELECT" ++ comma(read) ++ fr"FROM" ++ this.sql
    val sql = select ++ where(filter) ++ orderBy(sort)
    sql.query[Output].streamWithChunkSize(chunkSize)

object Table:
  type TableNameTag
  type Name = String @@ TableNameTag

  final case class Write(columns: List[Column[_]], values: Fragment):
    def sql: Fragment = parentheses(comma(columns)) ++ fr"VALUES" ++ parentheses(values)
