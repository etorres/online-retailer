package es.eriktorr
package commons.std

import cats.effect.IO
import munit.CatsEffectSuite

final class TSIDGenSuite extends CatsEffectSuite:
  test("should generate a random TSID"):
    TSIDGen[IO].randomTSID.map(_.toBytes.nn.length).assertEquals(8, "expected 8 bytes")
