package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait LongFormat {
  implicit val LongFormat: JsonFormat[Long] = new JsonFormat[Long] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Long = jsOpt match {
      case Some(js) => unbuilder.readLong(js)
      case None     => deserializationError("Expected JLong but found None")
    }

    override def write[J](obj: Long, builder: Builder[J]): Unit = {
      builder.writeLong(obj)
    }
  }
}