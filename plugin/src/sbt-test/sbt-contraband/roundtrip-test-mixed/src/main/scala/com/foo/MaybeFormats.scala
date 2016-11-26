package com.foo

import java.io.File

import com.example.Maybe
import sjsonnew.{ Builder, deserializationError, JsonFormat, Unbuilder }

trait MaybeFormats {
  implicit val maybeIntFormat: JsonFormat[Maybe[Integer]] = new JsonFormat[Maybe[Integer]] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Maybe[Integer] = {
      jsOpt match {
        case Some(js) if unbuilder.isJnull(js) => Maybe.nothing()
        case Some(js)                          => Maybe.just(unbuilder.readInt(js))
        case None                              => Maybe.nothing()
      }
    }

    override def write[J](obj: Maybe[Integer], builder: Builder[J]): Unit = {
      if (obj.isDefined) builder.writeInt(obj.get)
      else builder.writeNull()
    }
  }

  implicit val maybeStringFormat: JsonFormat[Maybe[String]] = new JsonFormat[Maybe[String]] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Maybe[String] = {
      jsOpt match {
        case Some(js) if unbuilder.isJnull(js) => Maybe.nothing()
        case Some(js)                          => Maybe.just(unbuilder.readString(js))
        case None                              => Maybe.nothing()
      }
    }

    override def write[J](obj: Maybe[String], builder: Builder[J]): Unit = {
      if (obj.isDefined) builder.writeString(obj.get)
      else builder.writeNull()
    }
  }
}
