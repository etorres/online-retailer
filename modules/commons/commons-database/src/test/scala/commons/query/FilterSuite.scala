package es.eriktorr
package commons.query

import commons.query.Column.{filterable, Filterable}
import commons.query.Filter.Combinator.{And, In, Not, Or}
import commons.query.Filter.Comparator.*
import commons.query.FilterSuite.*

import cats.collections.Range
import cats.effect.IO
import doobie.h2.H2Transactor
import doobie.implicits.given
import doobie.{ConnectionIO, Fragment, Meta}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF.forAllF
import org.scalacheck.{Gen, Test}

import scala.concurrent.ExecutionContext
import scala.util.Random

final class FilterSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  test("integer identities"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Equal(selected)).query[Int].unique,
      ).assertEquals(selected)

  test("enum identities"):
    forAllF(Gen.oneOf(TestEnum.values.toList)): selected =>
      given Filterable[TestEnum] = filterableTestEnum
      val items = TestEnum.values.toList
      run(
        select("col_enum", items, Equal(selected), true).query[TestEnum].unique,
      ).assertEquals(selected)

  test("integer greater than others"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (-2 to 0).toList.map(selected + _)
      run(
        select("col_int", items, Greater(selected - 1)).query[Int].unique,
      ).assertEquals(selected)

  test("integer greater or equal than others"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (-2 to 0).toList.map(selected + _)
      run(
        select("col_int", items, Greater(selected, true)).query[Int].unique,
      ).assertEquals(selected)

  test("integer less than others"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Less(selected + 1)).query[Int].unique,
      ).assertEquals(selected)

  test("integer less or equal than others"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Less(selected, true)).query[Int].unique,
      ).assertEquals(selected)

  test("between two values"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Between(Range(selected + 1, selected + 2)))
          .query[Int]
          .to[List]
          .map(_.sorted),
      ).assertEquals(List(selected + 1, selected + 2))

  test("pattern matching"):
    forAllF(Gen.oneOf(TestEnum.values.toList).map(_.toString)): selected =>
      given Filterable[String] = filterableString
      val items = List(selected, selected.patch(2, "XYZ", 0))
      run(
        select("col_string", items, Like(s"%$selected%"), true).query[String].unique,
      ).assertEquals(selected)

  test("conjunction"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, And(Greater(selected), Less(selected + 2)))
          .query[Int]
          .unique,
      ).assertEquals(selected + 1)

  test("disjunction"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Or(Equal(selected), Less(selected + 2)))
          .query[Int]
          .to[List]
          .map(_.sorted),
      ).assertEquals(List(selected, selected + 1))

  test("is in a list of values"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, In(selected, selected + 1, selected + 10000))
          .query[Int]
          .to[List]
          .map(_.sorted),
      ).assertEquals(List(selected, selected + 1))

  test("negation"):
    forAllF(Gen.choose(-1000, 1000)): selected =>
      given Filterable[Int] = filterableInt
      val items = (0 to 2).toList.map(selected + _)
      run(
        select("col_int", items, Not(Equal(selected)))
          .query[Int]
          .to[List]
          .map(_.sorted),
      ).assertEquals(List(selected + 1, selected + 2))

  private def select[T](column: String, items: List[T], filter: Filter, escape: Boolean = false) =
    val tempTable = Random
      .shuffle(items)
      .map(x => s"SELECT ${if escape then s"'$x'" else s"$x"} AS $column")
      .mkString(" UNION ALL ")
    Fragment.const(s"SELECT $column FROM ($tempTable) ") ++ QueryBuilder.where(filter)

  override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(3).withWorkers(1)

object FilterSuite:
  private def run[T](query: ConnectionIO[T])(using executionContext: ExecutionContext) =
    H2Transactor
      .newH2Transactor[IO](
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "sa",
        "",
        executionContext,
      )
      .use(query.transact)

  enum TestEnum:
    case CaseA, CaseB, CaseC

  object TestEnum:
    given Meta[TestEnum] = Meta[String].timap(TestEnum.valueOf)(_.toString)

  private val filterableInt = filterable[Int]("col_int")

  private val filterableString = filterable[String]("col_string")

  private val filterableTestEnum = filterable[TestEnum]("col_enum")
