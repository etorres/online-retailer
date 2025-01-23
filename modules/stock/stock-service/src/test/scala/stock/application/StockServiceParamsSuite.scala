package es.eriktorr
package stock.application

import cats.implicits.catsSyntaxEitherId
import com.monovore.decline.{Command, Help}
import munit.FunSuite

final class StockServiceParamsSuite extends FunSuite:
  test("should load default parameters"):
    assertEquals(
      Command(name = "name", header = "header")(StockServiceParams.opts).parse(List.empty),
      StockServiceParams(false).asRight[Help],
    )

  test("should load parameters from application arguments"):
    assertEquals(
      Command(name = "name", header = "header")(StockServiceParams.opts).parse(List("-v")),
      StockServiceParams(true).asRight[Help],
    )
