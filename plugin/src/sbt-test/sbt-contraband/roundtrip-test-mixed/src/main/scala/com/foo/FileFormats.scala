package com.foo

import java.io.File

import sjsonnew.{ Builder, deserializationError, JsonFormat, Unbuilder }

trait FileFormats {
  implicit val fileFormat: JsonFormat[File] = new JsonFormat[File] {
    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): File = {
      __jsOpt match {
        case Some(__js) => new File(unbuilder.readString(__js))
        case None     => deserializationError("Expected JsString but found None")
      }
    }

    override def write[J](obj: File, builder: Builder[J]): Unit = {
      builder.writeString(obj.getAbsolutePath)
    }
  }
}