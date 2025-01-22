package es.eriktorr
package electronics.application

import cats.implicits.catsSyntaxEitherId
import com.monovore.decline.{Command, Help}
import munit.FunSuite

final class ElectronicsServiceParamsSuite extends FunSuite:
  test("should load default parameters"):
    assertEquals(
      Command(name = "name", header = "header")(ElectronicsServiceParams.opts).parse(List.empty),
      ElectronicsServiceParams(false).asRight[Help],
    )

  test("should load parameters from application arguments"):
    assertEquals(
      Command(name = "name", header = "header")(ElectronicsServiceParams.opts).parse(List("-v")),
      ElectronicsServiceParams(true).asRight[Help],
    )
