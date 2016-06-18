package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait CharFormat extends JsonFormat[Char] {
  implicit val CharFormat: JsonFormat[Char] = new JsonFormat[Char] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Char = jsOpt match {
      case Some(js) =>
        val str = unbuilder.readString(js)
        if (str.length == 1) str.charAt(0)
        else deserializationError(s"Expected JString of length 1, found JString of length ${str.length}")
      case None =>
        deserializationError("Expected JString of length 1 but found None")
    }

    override def write[J](obj: Char, builder: Builder[J]): Unit = {
      builder.writeString(obj.toString)
    }
  }
}