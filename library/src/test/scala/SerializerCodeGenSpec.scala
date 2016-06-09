package sbt.datatype

import java.io.File

import org.specs2._
import NewSchema._

class SerializedCodeGenSpec extends GCodeGenSpec("Serializer") {

  val outputFile = new File("output.scala")
  val serializerPackage = Some("serializer")
  val serializerName = "Serializer"
  val serializerParents = Nil
  val genFileName = (_: Definition) => outputFile
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"

  override def enumerationGenerateSimple = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class simpleEnumerationExampleFormat extends JsonFormat[simpleEnumerationExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleEnumerationExample = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.readString(js) match {
        |          case "first" => simpleEnumerationExample.first
        |          case "second" => simpleEnumerationExample.second
        |        }
        |      case None =>
        |        deserializationError("Expected JsString but found None")
        |    }
        |  }
        |
        |  override def write[J](obj: simpleEnumerationExample, builder: Builder[J]): Unit = {
        |    val str = obj match {
        |      case simpleEnumerationExample.first => "first"
        |      case simpleEnumerationExample.second => "second"
        |    }
        |    builder.writeString(str)
        |  }
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateSimple = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val protocol = Protocol parse simpleProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class simpleProtocolExampleFormat extends JsonFormat[simpleProtocolExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleProtocolExample = {
        |    deserializationError("No known implementation of simpleProtocolExample.")
        |  }
        |  override def write[J](obj: simpleProtocolExample, builder: Builder[J]): Unit = {
        |    serializationError("No known implementation of simpleProtocolExample.")
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val simpleProtocolExampleFormat: JsonFormat[simpleProtocolExample] = new simpleProtocolExampleFormat()
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateOneChild = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val protocol = Protocol parse oneChildProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.serializer.Serializer._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class oneChildProtocolExampleFormat extends JsonFormat[oneChildProtocolExample] {
        |  private val format = unionFormat1[oneChildProtocolExample, childRecord]
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): oneChildProtocolExample = {
        |    format.read(jsOpt, unbuilder)
        |  }
        |  override def write[J](obj: oneChildProtocolExample, builder: Builder[J]): Unit = {
        |    format.write(obj, builder)
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.serializer.Serializer._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class childRecordFormat extends JsonFormat[childRecord] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): childRecord = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.beginObject(js)
        |        unbuilder.endObject()
        |        new childRecord()
        |      case None =>
        |        deserializationError("Expected JsObject but found None")
        |    }
        |  }
        |  override def write[J](obj: childRecord, builder: Builder[J]): Unit = {
        |    builder.beginObject()
        |    builder.endObject()
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val oneChildProtocolExampleFormat: JsonFormat[oneChildProtocolExample] = new oneChildProtocolExampleFormat()
        |  implicit val childRecordFormat: JsonFormat[childRecord] = new childRecordFormat()
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateNested = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val protocol = Protocol parse nestedProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |import _root_.serializer.Serializer._
        |class nestedProtocolExampleFormat extends JsonFormat[nestedProtocolExample] {
        |  private val format = unionFormat1[nestedProtocolExample, nestedProtocol]
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): nestedProtocolExample = {
        |    format.read(jsOpt, unbuilder)
        |  }
        |  override def write[J](obj: nestedProtocolExample, builder: Builder[J]): Unit = {
        |    format.write(obj, builder)
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class nestedProtocolFormat extends JsonFormat[nestedProtocol] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): nestedProtocol = {
        |    deserializationError("No known implementation of nestedProtocol.")
        |  }
        |  override def write[J](obj: nestedProtocol, builder: Builder[J]): Unit = {
        |    serializationError("No known implementation of nestedProtocol.")
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val nestedProtocolExampleFormat: JsonFormat[nestedProtocolExample] = new nestedProtocolExampleFormat()
        |  implicit val nestedProtocolFormat: JsonFormat[nestedProtocol] = new nestedProtocolFormat()
        |}""".stripMargin.unindent)
  }

  def protocolGenerateAbstractMethods = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val schema = Schema parse generateArgDocExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |class generateArgDocExampleFormat extends JsonFormat[generateArgDocExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): generateArgDocExample = {
        |    deserializationError("No known implementation of generateArgDocExample.")
        |  }
        |  override def write[J](obj: generateArgDocExample, builder: Builder[J]): Unit = {
        |    serializationError("No known implementation of generateArgDocExample.")
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val generateArgDocExampleFormat: JsonFormat[generateArgDocExample] = new generateArgDocExampleFormat()
        |}""".stripMargin.unindent)
  }

  override def recordGenerateSimple = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |import _root_.serializer.Serializer._
        |class simpleRecordExampleFormat extends JsonFormat[simpleRecordExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleRecordExample = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.beginObject(js)
        |        val field = unbuilder.readField[type]("field")
        |        unbuilder.endObject()
        |        new simpleRecordExample(field)
        |      case None =>
        |        deserializationError("Expected JsObject but found None")
        |    }
        |  }
        |  override def write[J](obj: simpleRecordExample, builder: Builder[J]): Unit = {
        |    builder.beginObject()
        |    builder.addField("field", obj.field)
        |    builder.endObject()
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val simpleRecordExampleFormat: JsonFormat[simpleRecordExample] = new simpleRecordExampleFormat()
        |}""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |import _root_.serializer.Serializer._
        |class growableAddOneFieldFormat extends JsonFormat[growableAddOneField] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): growableAddOneField = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.beginObject(js)
        |        val field = unbuilder.readField[Int]("field")
        |        unbuilder.endObject()
        |        new growableAddOneField(field)
        |      case None =>
        |        deserializationError("Expected JsObject but found None")
        |    }
        |  }
        |  override def write[J](obj: growableAddOneField, builder: Builder[J]): Unit = {
        |    builder.beginObject()
        |    builder.addField("field", obj.field)
        |    builder.endObject()
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val growableAddOneFieldFormat: JsonFormat[growableAddOneField] = new growableAddOneFieldFormat()
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferences = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val schema = Schema parse primitiveTypesExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |import _root_.serializer.Serializer._
        |class primitiveTypesExampleFormat extends JsonFormat[primitiveTypesExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): primitiveTypesExample = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.beginObject(js)
        |        val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |        val lazyInteger = unbuilder.readField[Int]("lazyInteger")
        |        val arrayInteger = unbuilder.readField[Array[Int]]("arrayInteger")
        |        val lazyArrayInteger = unbuilder.readField[Array[Int]]("lazyArrayInteger")
        |        unbuilder.endObject()
        |        new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, lazyArrayInteger)
        |      case None =>
        |        deserializationError("Expected JsObject but found None")
        |    }
        |  }
        |  override def write[J](obj: primitiveTypesExample, builder: Builder[J]): Unit = {
        |    builder.beginObject()
        |    builder.addField("simpleInteger", obj.simpleInteger)
        |    builder.addField("lazyInteger", obj.lazyInteger)
        |    builder.addField("arrayInteger", obj.arrayInteger)
        |    builder.addField("lazyArrayInteger", obj.lazyArrayInteger)
        |    builder.endObject()
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val primitiveTypesExampleFormat: JsonFormat[primitiveTypesExample] = new primitiveTypesExampleFormat()
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val schema = Schema parse primitiveTypesNoLazyExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |import _root_.serializer.Serializer._
        |class primitiveTypesNoLazyExampleFormat extends JsonFormat[primitiveTypesNoLazyExample] {
        |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): primitiveTypesNoLazyExample = {
        |    jsOpt match {
        |      case Some(js) =>
        |        unbuilder.beginObject(js)
        |        val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |        val arrayInteger = unbuilder.readField[Array[Int]]("arrayInteger")
        |        unbuilder.endObject()
        |        new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |      case None =>
        |        deserializationError("Expected JsObject but found None")
        |    }
        |  }
        |  override def write[J](obj: primitiveTypesNoLazyExample, builder: Builder[J]): Unit = {
        |    builder.beginObject()
        |    builder.addField("simpleInteger", obj.simpleInteger)
        |    builder.addField("arrayInteger", obj.arrayInteger)
        |    builder.endObject()
        |  }
        |}
        |import _root_.sjsonnew._
        |import _root_.sjsonnew.BasicJsonProtocol._
        |object Serializer  {
        |  implicit val primitiveTypesNoLazyExampleFormat: JsonFormat[primitiveTypesNoLazyExample] = new primitiveTypesNoLazyExampleFormat()
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(completeExampleCodeSerializer.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val gen = new SerializerCodeGen(genFileName, serializerPackage, serializerName, serializerParents, instantiateJavaLazy)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.withoutEmptyLines must containTheSameElementsAs(completeExampleCodeSerializer.withoutEmptyLines)
  }

}
