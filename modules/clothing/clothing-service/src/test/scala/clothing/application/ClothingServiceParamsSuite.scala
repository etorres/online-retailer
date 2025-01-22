package es.eriktorr
package clothing.application

import cats.implicits.catsSyntaxEitherId
import com.monovore.decline.{Command, Help}
import munit.FunSuite

final class ClothingServiceParamsSuite extends FunSuite:
  test("should load default parameters"):
    assertEquals(
      Command(name = "name", header = "header")(ClothingServiceParams.opts).parse(List.empty),
      ClothingServiceParams(false).asRight[Help],
    )

  test("should load parameters from application arguments"):
    assertEquals(
      Command(name = "name", header = "header")(ClothingServiceParams.opts).parse(List("-v")),
      ClothingServiceParams(true).asRight[Help],
    )
