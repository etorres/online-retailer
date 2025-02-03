package es.eriktorr
package taxes

import commons.query.Filter.Comparator.Equal
import commons.query.Filter.NoFilter
import commons.query.Sort.NoSort
import commons.query.{Filter, Sort}
import commons.spec.PostgresSuite
import taxes.TaxesRepositorySuite.{findTaxesByIdTestCaseGen, selectAllTestCaseGen, TestCase}
import taxes.db.TaxRowGenerators.taxRowsGen
import taxes.db.TaxesTable.{TaxRow, given}

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.toFoldableOps
import es.eriktorr.taxes.db.TestTaxesRepository
import org.scalacheck.Gen
import org.scalacheck.effect.PropF.forAllF

final class TaxesRepositorySuite extends PostgresSuite:
  test("should find a tax by its id"):
    testWith(
      findTaxesByIdTestCaseGen,
      (testee, filter, _) => testee.findTaxBy(filter.to).value,
    )

  test("should list all taxes"):
    testWith(
      selectAllTestCaseGen,
      (testee, filter, sort) => testee.selectTaxesBy(filter, sort).compile.toList,
    )

  private def testWith[A, B <: Filter](
      testCaseGen: Gen[TestCase[A, B]],
      run: (TaxesRepository, B, Sort) => IO[A],
  ) =
    forAllF(testCaseGen):
      case TestCase(taxRows, filter, sort, expected, diff) =>
        testTransactor.resource.use: transactor =>
          val testTaxRepository = TestTaxesRepository(transactor)
          val testee = TaxesRepository.Postgres(transactor)
          (for
            _ <- taxRows.traverse_(testTaxRepository.add)
            obtained <- run(testee, filter, sort)
          yield diff(obtained)).assertEquals(diff(expected))

object TaxesRepositorySuite:
  final private case class TestCase[A, B <: Filter](
      taxRows: List[TaxRow],
      filter: B,
      sort: Sort,
      expected: A,
      diff: A => A,
  )

  private val sortById = (taxes: List[Tax]) => taxes.sortBy(_.id)

  private val toStandardUnits = (tax: Tax) =>
    val roundedRate = BigDecimal(tax.rate).setScale(2, BigDecimal.RoundingMode.HALF_UP).doubleValue
    tax.copy(rate = Tax.Rate.applyUnsafe(roundedRate))

  private def unRow(row: TaxRow) =
    toStandardUnits(Tax(row.id, row.tax, row.rate))

  private val findTaxesByIdTestCaseGen = for
    taxRows <- taxRowsGen
    (selectedRow, otherRows) =
      val nel = NonEmptyList.fromListUnsafe(taxRows)
      nel.head -> nel.tail
    filter = Equal(selectedRow.id)
    expected = Option(selectedRow).map(unRow)
  yield TestCase(taxRows, filter, NoSort, expected, identity)

  private val selectAllTestCaseGen = for
    taxRows <- taxRowsGen
    expected = taxRows.map(unRow)
  yield TestCase(taxRows, NoFilter, NoSort, expected, sortById)
