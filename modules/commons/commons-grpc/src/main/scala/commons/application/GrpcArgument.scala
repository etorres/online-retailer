package es.eriktorr
package commons.application

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.Argument

trait GrpcArgument:
  given hostArgument: Argument[Host] = new Argument[Host]:
    override def read(string: String): ValidatedNel[String, Host] = Host.fromString(string) match
      case Some(value) => value.validNel
      case None => s"Invalid host: $string".invalidNel

    override def defaultMetavar: String = "host"
  end hostArgument

  given portArgument: Argument[Port] = new Argument[Port]:
    override def read(string: String): ValidatedNel[String, Port] = Port.fromString(string) match
      case Some(value) => value.validNel
      case None => s"Invalid port: $string".invalidNel

    override def defaultMetavar: String = "port"
  end portArgument

object GrpcArgument extends GrpcArgument
