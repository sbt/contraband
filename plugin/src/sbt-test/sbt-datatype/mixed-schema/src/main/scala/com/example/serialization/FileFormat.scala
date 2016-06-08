package com.example.serialization

import java.io.File

import sjsonnew._

trait FileFormat {
  implicit val fileFormat: JsonFormat[File] = new JsonFormat[File] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): File = {
      jsOpt match {
        case Some(js) => new File(unbuilder.readString(js))
        case None     => deserializationError("Expected JsString but found None")
      }
    }

    override def write[J](obj: File, builder: Builder[J]): Unit = {
      builder.writeString(obj.getAbsolutePath)
    }
  }
}