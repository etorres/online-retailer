package es.eriktorr
package commons

import cats.implicits.toShow
import munit.FunSuite

import scala.runtime.stdLibPatches.Predef.assert

final class SecretSuite extends FunSuite:
  test("should protect a secret from disclosure"):
    val secret = Secret("s3c4Et")
    assert(secret.value == "s3c4Et", "secret value")
    assert(secret.valueHash == "99559ca663009414306a65eb5a3dabb502d5129b", "secret hash")
    assert(secret.valueShortHash == "99559ca", "secret short hash")
    assert(secret.toString == s"Secret(99559ca)", "secret to string")
    assert(secret.show == s"Secret(99559ca)", "secret to string")
    secret match
      case Secret(value) => assert(value == "s3c4Et", "secret unapply")
