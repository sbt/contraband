package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait ByteFormat {
  implicit val ByteFormat: JsonFormat[Byte] = new JsonFormat[Byte] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Byte = jsOpt match {
      case Some(js) => unbuilder.readInt(js).toByte
      case None     => deserializationError("Expected JByte but found None")
    }

    override def write[J](obj: Byte, builder: Builder[J]): Unit = {
      builder.writeInt(obj.toByte)
    }
  }
}