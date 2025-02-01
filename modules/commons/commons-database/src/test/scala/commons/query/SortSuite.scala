package es.eriktorr
package commons.query

import commons.query.Column.sortable
import commons.query.Sort.{Ascending, Descending}
import commons.query.SortSuite.{run, sortableInt}

import cats.effect.IO
import doobie.h2.H2Transactor
import doobie.implicits.given
import doobie.{ConnectionIO, Fragment}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF
import com.softwaremill.tagging.*

import scala.concurrent.ExecutionContext
import scala.util.Random

final class SortSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("sort in ascending order"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      val items = (0 to 2).toList.map(selected + _)
      run(
        orderBy("col_int", items, Ascending(sortableInt)).query[Int].to[List],
      ).assertEquals(items)

  test("sort in descending order"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      val items = (0 to 2).toList.map(selected + _)
      run(
        orderBy("col_int", items, Descending(sortableInt)).query[Int].to[List],
      ).assertEquals(items.sorted.reverse)

  private def orderBy[T](column: String, items: List[T], sort: Sort) =
    val tempTable = Random
      .shuffle(items)
      .map(x => s"SELECT $x AS $column")
      .mkString(" UNION ALL ")
    Fragment.const(s"SELECT $column FROM ($tempTable) ") ++ QueryBuilder.orderBy(sort)

object SortSuite:
  private def run[T](query: ConnectionIO[T])(using executionContext: ExecutionContext) =
    H2Transactor
      .newH2Transactor[IO](
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "sa",
        "",
        executionContext,
      )
      .use(query.transact)

  given Table.Name = "table".taggedWith[Table.TableNameTag]

  private val sortableInt = sortable[Int]("col_int")
