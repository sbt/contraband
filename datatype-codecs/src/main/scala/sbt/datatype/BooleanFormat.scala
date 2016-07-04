package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait BooleanFormat {
  implicit val BooleanFormat: JsonFormat[Boolean] = new JsonFormat[Boolean] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Boolean = jsOpt match {
      case Some(js) => unbuilder.readBoolean(js)
      case None     => deserializationError("Expected JBoolean but found None")
    }

    override def write[J](obj: Boolean, builder: Builder[J]): Unit = {
      builder.writeBoolean(obj)
    }
  }
}