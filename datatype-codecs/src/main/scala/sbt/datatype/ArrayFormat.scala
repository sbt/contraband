package sbt.datatype

import sjsonnew.{ deserializationError, Builder, JsonFormat, Unbuilder }

trait ArrayFormat {

  implicit def ArrayFormat[T : JsonFormat : ClassManifest] = new JsonFormat[Array[T]] {
    val elementsFormat: JsonFormat[T] = implicitly
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Array[T] = jsOpt match {
      case Some(js) =>
        val size = unbuilder.beginArray(js)
        val array = Array.fill[T](size)(elementsFormat.read(Some(unbuilder.nextElement), unbuilder))
        unbuilder.endArray()
        array
      case None     => deserializationError("Expected JArray but found None")
    }

    override def write[J](obj: Array[T], builder: Builder[J]): Unit = {
      builder.beginArray()
      obj foreach { e => elementsFormat.write(e, builder) }
      builder.endArray
    }
  }
}