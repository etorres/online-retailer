package es.eriktorr
package commons

import cats.implicits.toShow
import cats.{Eq, Show}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import scala.annotation.tailrec

/** Secret configuration value which might contain sensitive details.
  *
  * (Adapted from Ciris: a Functional Configurations for Scala).
  *
  * When a secret configuration value is shown, the value is replaced by the first 7 characters of
  * the SHA-1 hash for the value. This short SHA-1 hash is available as [[Secret#valueShortHash]],
  * and the full SHA-1 hash is available as [[Secret#valueHash]]. The underlying configuration value
  * is available as [[Secret#value]].
  *
  * @see
  *   [[https://cir.is/docs/configurations#secrets Ciris - Secrets]]
  */
sealed trait Secret[+A]:
  def value: A
  def valueHash: String
  def valueShortHash: String
end Secret

object Secret:
  final def apply[A](value: A)(using show: Show[A]): Secret[A] =
    val _value = value
    new Secret[A]:
      final override val value: A = _value

      final override def valueHash: String = sha1Hex(value.show)

      final override def valueShortHash: String = valueHash.take(7)

      final override def hashCode: Int = value.hashCode

      import compiletime.asMatchable

      @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.AsInstanceOf"))
      final override def equals(that: Any): Boolean = that.asMatchable match
        case Secret(thatValue) => value == thatValue
        case _ => false

      final override def toString: String = s"Secret($valueShortHash)"

  final def unapply[A](secret: Secret[A]): Some[A] = Some(secret.value)

  given secretEq[A](using eq: Eq[A]): Eq[Secret[A]] = Eq.by(_.value)

  given secretShow[A]: Show[Secret[A]] = Show.fromToString

  final private def sha1Hex(string: String) =
    import scala.language.unsafeNulls
    val stringBytes = string.getBytes(StandardCharsets.UTF_8)
    val sha1 = MessageDigest.getInstance("SHA-1").digest(stringBytes)

    @tailrec
    def hexFrom(bytes: List[Byte], stringBuilder: StringBuilder): String = bytes match
      case Nil => stringBuilder.toString()
      case ::(head, next) =>
        hexFrom(next, stringBuilder.append(String.format("%02x", Byte.box(head))))

    hexFrom(sha1.toList, StringBuilder())
end Secret
