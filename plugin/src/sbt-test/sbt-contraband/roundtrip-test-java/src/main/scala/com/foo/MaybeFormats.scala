package com.foo

import java.io.File

import com.example.Maybe
import sjsonnew.{ Builder, deserializationError, JsonFormat, Unbuilder }

trait MaybeFormats {
  private[this] type JF[A] = JsonFormat[A] // simple alias for reduced verbosity

  implicit object IntegerJsonFormat extends JsonFormat[Integer] {
    def write[J](x: Integer, builder: Builder[J]): Unit =
      builder.writeInt(x)
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Integer =
      jsOpt match {
        case Some(js) => unbuilder.readInt(js)
        case None     => 0
      }
  }

  implicit def maybeFormat[A: JF]: JF[Maybe[A]] = new MaybeFormat[A]

  final class MaybeFormat[A :JF] extends JF[Maybe[A]] {
    val elemFormat: JsonFormat[A] = implicitly[JsonFormat[A]]

    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Maybe[A] =
      jsOpt match {
        case Some(js) if unbuilder.isJnull(js) => Maybe.nothing()
        case Some(js)                          => Maybe.just(elemFormat.read(jsOpt, unbuilder))
        case None                              => Maybe.nothing()
      }
    override def addField[J](name: String, obj: Maybe[A], builder: Builder[J]): Unit =
      if (obj.isDefined) {
        builder.addFieldName(name)
        write(obj, builder)
      }
      else ()
    override def write[J](obj: Maybe[A], builder: Builder[J]): Unit =
      if (obj.isDefined) elemFormat.write(obj.get, builder)
      else builder.writeNull()
  }
}
