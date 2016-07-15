package sbt.datatype

import java.io.File

import org.specs2._
import NewSchema._

class CodecCodeGenSpec extends GCodeGenSpec("Codec") {

  def codecCodeGenSpec = s2"""
    This is additional part of the specification that are relevant only to the codec code generator.

    The full codec object should
      include the codec of all protocol defined in the schema          $fullCodecCheck
  """

  override def is = super.is append codecCodeGenSpec

  val codecParents = Nil
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"
  val javaOption = "com.example.Option"
  val scalaArray = "Vector"
  val formatsForType: TpeRef => List[String] = CodecCodeGen.formatsForType

  override def enumerationGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait SimpleEnumerationExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleEnumerationExampleFormat: JsonFormat[_root_.simpleEnumerationExample] = new JsonFormat[_root_.simpleEnumerationExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleEnumerationExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.readString(js) match {
        |            case "first" => _root_.simpleEnumerationExample.first
        |            case "second" => _root_.simpleEnumerationExample.second
        |          }
        |        case None =>
        |          deserializationError("Expected JsString but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: _root_.simpleEnumerationExample, builder: Builder[J]): Unit = {
        |      val str = obj match {
        |        case _root_.simpleEnumerationExample.first => "first"
        |        case _root_.simpleEnumerationExample.second => "second"
        |      }
        |      builder.writeString(str)
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val intf = Interface parse simpleInterfaceExample
    val code = gen generate intf

    code.head._2.unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait SimpleInterfaceExampleFormats {
        |  implicit lazy val simpleInterfaceExampleFormat: JsonFormat[_root_.simpleInterfaceExample] = new JsonFormat[_root_.simpleInterfaceExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleInterfaceExample = {
        |      deserializationError("No known implementation of simpleInterfaceExample.")
        |    }
        |    override def write[J](obj: _root_.simpleInterfaceExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of simpleInterfaceExample.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateOneChild = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val intf = Interface parse oneChildInterfaceExample
    val code = gen generate intf

    (code(new File("generated", "oneChildInterfaceExampleFormats.scala")).unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait OneChildInterfaceExampleFormats { self: generated.ChildRecordFormats with sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val oneChildInterfaceExampleFormat: JsonFormat[_root_.oneChildInterfaceExample] = unionFormat1[_root_.oneChildInterfaceExample, _root_.childRecord]
        |}""".stripMargin.unindent)) and
    (code(new File("generated", "childRecordFormats.scala")).unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait ChildRecordFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val childRecordFormat: JsonFormat[_root_.childRecord] = new JsonFormat[_root_.childRecord] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.childRecord = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val field = unbuilder.readField[Int]("field")
        |          val x = unbuilder.readField[Int]("x")
        |          unbuilder.endObject()
        |          new _root_.childRecord(field, x)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.childRecord, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.addField("x", obj.x)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent))
  }

  override def interfaceGenerateNested = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val intf = Interface parse nestedInterfaceExample
    val code = gen generate intf

    (code(new File("generated", "nestedProtocolExampleFormats.scala")).unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait NestedProtocolExampleFormats { self: generated.NestedProtocolFormats with sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val nestedProtocolExampleFormat: JsonFormat[_root_.nestedProtocolExample] = unionFormat1[_root_.nestedProtocolExample, _root_.nestedProtocol]
        |}""".stripMargin.unindent)) and
    (code(new File("generated", "nestedProtocolFormats.scala")).unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait NestedProtocolFormats {
        |  implicit lazy val nestedProtocolFormat: JsonFormat[_root_.nestedProtocol] = new JsonFormat[_root_.nestedProtocol] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.nestedProtocol = {
        |      deserializationError("No known implementation of nestedProtocol.")
        |    }
        |    override def write[J](obj: _root_.nestedProtocol, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of nestedProtocol.")
        |    }
        |  }
        |}""".stripMargin.unindent))
  }

  def interfaceGenerateMessages = {
    val schema = Schema parse generateArgDocExample
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait GenerateArgDocExampleFormats {
        |  implicit lazy val generateArgDocExampleFormat: JsonFormat[_root_.generateArgDocExample] = new JsonFormat[_root_.generateArgDocExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.generateArgDocExample = {
        |      deserializationError("No known implementation of generateArgDocExample.")
        |    }
        |    override def write[J](obj: _root_.generateArgDocExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of generateArgDocExample.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordGenerateSimple = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait SimpleRecordExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleRecordExampleFormat: JsonFormat[_root_.simpleRecordExample] = new JsonFormat[_root_.simpleRecordExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.simpleRecordExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val field = unbuilder.readField[java.net.URL]("field")
        |          unbuilder.endObject()
        |          new _root_.simpleRecordExample(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.simpleRecordExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """package generated
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait GrowableAddOneFieldFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val growableAddOneFieldFormat: JsonFormat[_root_.growableAddOneField] = new JsonFormat[_root_.growableAddOneField] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.growableAddOneField = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val field = unbuilder.readField[Int]("field")
        |          unbuilder.endObject()
        |          new _root_.growableAddOneField(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.growableAddOneField, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferences = {
    val schema = Schema parse primitiveTypesExample
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait PrimitiveTypesExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesExampleFormat: JsonFormat[_root_.primitiveTypesExample] = new JsonFormat[_root_.primitiveTypesExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.primitiveTypesExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val lazyInteger = unbuilder.readField[Int]("lazyInteger")
        |          val arrayInteger = unbuilder.readField[Vector[Int]]("arrayInteger")
        |          val optionInteger = unbuilder.readField[Option[Int]]("optionInteger")
        |          val lazyArrayInteger = unbuilder.readField[Vector[Int]]("lazyArrayInteger")
        |          val lazyOptionInteger = unbuilder.readField[Option[Int]]("lazyOptionInteger")
        |          unbuilder.endObject()
        |          new _root_.primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: _root_.primitiveTypesExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("lazyInteger", obj.lazyInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.addField("optionInteger", obj.optionInteger)
        |      builder.addField("lazyArrayInteger", obj.lazyArrayInteger)
        |      builder.addField("lazyOptionInteger", obj.lazyOptionInteger)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val schema = Schema parse primitiveTypesNoLazyExample
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait PrimitiveTypesNoLazyExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesNoLazyExampleFormat: JsonFormat[_root_.primitiveTypesNoLazyExample] = new JsonFormat[_root_.primitiveTypesNoLazyExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.primitiveTypesNoLazyExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val arrayInteger = unbuilder.readField[Vector[Int]]("arrayInteger")
        |          unbuilder.endObject()
        |          new _root_.primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: _root_.primitiveTypesNoLazyExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val schema = Schema parse completeExample
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema
    // println(code.values.mkString)
    code.values.mkString.unindent must containTheSameElementsAs(completeExampleCodeCodec.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = Schema parse completeExample
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema

    code.values.mkString.withoutEmptyLines must containTheSameElementsAs(completeExampleCodeCodec.withoutEmptyLines)
  }

  def fullCodecCheck = {
    val schema = Schema parse s"""{
                                 |  "types": [
                                 |    {
                                 |      "name": "Greeting",
                                 |      "target": "Java",
                                 |      "type": "interface"
                                 |    }
                                 |  ]
                                 |}""".stripMargin
    val gen = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, schema :: Nil)
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait GreetingFormats {
        |  implicit lazy val GreetingFormat: JsonFormat[_root_.Greeting] = new JsonFormat[_root_.Greeting] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): _root_.Greeting = {
        |      deserializationError("No known implementation of Greeting.")
        |    }
        |    override def write[J](obj: _root_.Greeting, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of Greeting.")
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

}
