package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait ShortFormat {
  implicit val ShortFormat: JsonFormat[Short] = new JsonFormat[Short] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Short = jsOpt match {
      case Some(js) => unbuilder.readInt(js).toShort
      case None     => deserializationError("Expected JShort but found None")
    }

    override def write[J](obj: Short, builder: Builder[J]): Unit = {
      builder.writeInt(obj.toShort)
    }
  }
}