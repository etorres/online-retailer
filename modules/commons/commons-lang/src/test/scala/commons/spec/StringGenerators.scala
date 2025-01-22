package es.eriktorr
package commons.spec

import org.scalacheck.Gen

object StringGenerators:
  private val defaultMaxLength = 128

  def alphaNumericStringBetween(minLength: Int, maxLength: Int): Gen[String] =
    stringBetween(minLength, maxLength, Gen.alphaNumChar)

  def alphaNumericStringShorterThan(maxLength: Int): Gen[String] =
    stringShorterThan(maxLength, Gen.alphaNumChar)

  def alphaLowerStringBetween(minLength: Int, maxLength: Int): Gen[String] =
    stringBetween(minLength, maxLength, Gen.alphaLowerChar)

  val nonEmptyAlphaNumericStringGen: Gen[String] =
    nonEmptyStringShorterThan(defaultMaxLength, Gen.alphaNumChar)

  private def nonEmptyStringShorterThan(maxLength: Int, charGen: Gen[Char]): Gen[String] =
    stringBetween(1, maxLength, charGen)

  private def stringBetween(minLength: Int, maxLength: Int, charGen: Gen[Char]): Gen[String] =
    for
      stringLength <- Gen.choose(minLength, maxLength)
      string <- stringOfLength(stringLength, charGen)
    yield string

  private def stringOfLength(length: Int, charGen: Gen[Char]): Gen[String] =
    Gen.listOfN(length, charGen).map(_.mkString)

  private def stringShorterThan(maxLength: Int, charGen: Gen[Char]): Gen[String] =
    stringBetween(0, maxLength, charGen)
