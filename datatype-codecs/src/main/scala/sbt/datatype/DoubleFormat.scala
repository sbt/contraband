package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait DoubleFormat {
  implicit val DoubleFormat: JsonFormat[Double] = new JsonFormat[Double] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Double = jsOpt match {
      case Some(js) => unbuilder.readDouble(js)
      case None     => deserializationError("Expected JDouble but found None")
    }

    override def write[J](obj: Double, builder: Builder[J]): Unit = {
      builder.writeDouble(obj)
    }
  }
}