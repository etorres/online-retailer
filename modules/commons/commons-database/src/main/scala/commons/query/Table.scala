package es.eriktorr
package commons.query

import commons.query.QueryBuilder.comma

import com.softwaremill.tagging.*
import doobie.Fragment
import doobie.implicits.*
import doobie.util.fragments.parentheses

trait Table[T]:
  implicit val name: Table.Name

  def sql: Fragment = Fragment.const(name)
  def read: List[Column[_]]
  def write(value: T): Table.Write

object Table:
  type TableNameTag
  type Name = String @@ TableNameTag

  final case class Write(columns: List[Column[_]], values: Fragment):
    def sql: Fragment = parentheses(comma(columns)) ++ fr"VALUES" ++ parentheses(values)
