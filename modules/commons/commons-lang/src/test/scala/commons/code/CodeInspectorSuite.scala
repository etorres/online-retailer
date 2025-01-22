package es.eriktorr
package commons.code

import commons.code.CodeInspectorSuite.TestCase
import commons.code.CodeInspector.fieldNames

import munit.FunSuite

final class CodeInspectorSuite extends FunSuite:
  test("should list the field names of a case class"):
    val obtained = fieldNames[TestCase]
    assertEquals(obtained, List("name", "age", "address"))

object CodeInspectorSuite:
  final private case class TestCase(name: String, age: Int, address: String)
