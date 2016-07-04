package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait IntFormat {
  implicit val IntFormat: JsonFormat[Int] = new JsonFormat[Int] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Int = jsOpt match {
      case Some(js) => unbuilder.readInt(js)
      case None     => deserializationError("Expected JInt but found None")
    }

    override def write[J](obj: Int, builder: Builder[J]): Unit = {
      builder.writeInt(obj)
    }
  }
}