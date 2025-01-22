package es.eriktorr
package commons.db

import commons.error.HandledError

import cats.effect.IO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateErrorResult

import javax.sql.DataSource

final class JdbcMigrator(dataSource: DataSource):
  import scala.language.unsafeNulls

  def migrate: IO[Unit] =
    IO.blocking {
      val flyway = Flyway
        .configure()
        .dataSource(dataSource)
        .failOnMissingLocations(true)
        .load()
      flyway.migrate()
    }.flatMap:
      case errorResult: MigrateErrorResult =>
        IO.raiseError(JdbcMigrator.MigrationFailed(errorResult))
      case other => IO.unit

object JdbcMigrator:
  import scala.language.unsafeNulls

  sealed abstract class JdbcMigrationError(message: String) extends HandledError(message)

  final case class MigrationFailed(errorResult: MigrateErrorResult)
      extends JdbcMigrationError(
        s"message: ${errorResult.error.message}, stackTrace: ${errorResult.error.stackTrace}",
      )
