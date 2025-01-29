package es.eriktorr
package commons.query

trait Row[A, B] extends Row.ToRow[A, B] with Row.FromRow[B, A]

object Row:
  trait ToRow[A, B]:
    def row(a: A): B

  trait FromRow[B, A]:
    def unRow(b: B): A

  extension [A, B](self: A)
    def row(using transformer: ToRow[A, B]): B =
      transformer.row(self)

  extension [B, A](self: A)
    def unRow(using transformer: FromRow[A, B]): B =
      transformer.unRow(self)

  extension [A](self: List[A])
    def row[B](using transformer: ToRow[A, B]): List[B] =
      self.map: a =>
        transformer.row(a)

  extension [B](self: List[B])
    def unRow[A](using transformer: FromRow[B, A]): List[A] =
      self.map: b =>
        transformer.unRow(b)

  extension [A](self: Option[A])
    def row[B](using transformer: ToRow[A, B]): Option[B] =
      self.map: a =>
        transformer.row(a)

  extension [B](self: Option[B])
    def unRow[A](using transformer: FromRow[B, A]): Option[A] =
      self.map: b =>
        transformer.unRow(b)
