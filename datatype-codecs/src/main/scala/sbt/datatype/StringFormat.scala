package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait StringFormat {
  implicit val StringFormat: JsonFormat[String] = new JsonFormat[String] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): String = jsOpt match {
      case Some(js) => unbuilder.readString(js)
      case None     => deserializationError("Expected JString but found None")
    }

    override def write[J](obj: String, builder: Builder[J]): Unit = {
      builder.writeString(obj)
    }
  }
}