package es.eriktorr
package commons

import LoggingSuite.TestLog

import cats.effect.IO
import io.circe.parser.parse
import io.circe.{Decoder, HCursor}
import munit.CatsEffectSuite
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

final class LoggingSuite extends CatsEffectSuite:
  test("should write structured logs"):
    val targetPath = os.pwd / "target"
    val logsPath = targetPath / "logs" / "test.log"
    for
      _ <- IO
        .blocking(os.isDir(targetPath))
        .ifM(
          IO.unit,
          IO.raiseError(new RuntimeException("target directory not found")),
        )
      _ <- IO.blocking(os.remove(logsPath)).void
      logger <- Slf4jLogger.fromName[IO]("test-logger")
      _ <- logger.info(Map("customField" -> "Custom content"))("Testing Slf4jLogger")
      _ <- IO
        .race(IO.sleep(10.seconds), IO.blocking(os.exists(logsPath)).iterateUntil(_ == true))
        .void
      rawJsonLog <- IO.blocking(os.read(logsPath))
      result <- IO.delay(parse(rawJsonLog).flatMap(_.as[TestLog]))
      expected = TestLog("Custom content", "INFO", "Testing Slf4jLogger")
    yield assertEquals(result, Right(expected))

object LoggingSuite:
  final private case class TestLog(
      customField: String,
      logLevel: String,
      message: String,
  )

  private given Decoder[TestLog] = (cursor: HCursor) =>
    for
      customField <- cursor.downField("customField").as[String]
      logLevel <- cursor.downField("log.level").as[String]
      message <- cursor.downField("message").as[String]
    yield TestLog(
      customField = customField,
      logLevel = logLevel,
      message = message,
    )
