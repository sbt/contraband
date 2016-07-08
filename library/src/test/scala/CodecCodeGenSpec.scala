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


  val outputFile = new File("output.scala")
  val codecName = "Codec"
  val codecNamespace = None
  val codecParents = Nil
  val genFileName = (_: Definition) => outputFile
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"
  val formatsForType = CodecCodeGen.formatsForType

  override def enumerationGenerateSimple = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait simpleEnumerationExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleEnumerationExampleFormat: JsonFormat[simpleEnumerationExample] = new JsonFormat[simpleEnumerationExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleEnumerationExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.readString(js) match {
        |            case "first" => simpleEnumerationExample.first
        |            case "second" => simpleEnumerationExample.second
        |          }
        |        case None =>
        |          deserializationError("Expected JsString but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: simpleEnumerationExample, builder: Builder[J]): Unit = {
        |      val str = obj match {
        |        case simpleEnumerationExample.first => "first"
        |        case simpleEnumerationExample.second => "second"
        |      }
        |      builder.writeString(str)
        |    }
        |  }
        |}
        |trait Codec { self: _root_.simpleEnumerationExampleFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.simpleEnumerationExampleFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def protocolGenerateSimple = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val protocol = Interface parse simpleProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait simpleProtocolExampleFormats {
        |  implicit lazy val simpleProtocolExampleFormat: JsonFormat[simpleProtocolExample] = new JsonFormat[simpleProtocolExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleProtocolExample = {
        |      deserializationError("No known implementation of simpleProtocolExample.")
        |    }
        |    override def write[J](obj: simpleProtocolExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of simpleProtocolExample.")
        |    }
        |  }
        |}
        |trait Codec { self: _root_.simpleProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.typeFormats => }
        |object Codec extends Codec with _root_.simpleProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.typeFormats""".stripMargin.unindent)
  }

  override def protocolGenerateOneChild = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val protocol = Interface parse oneChildProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait oneChildProtocolExampleFormats { self: sjsonnew.BasicJsonProtocol with _root_.childRecordFormats =>
        |  implicit lazy val oneChildProtocolExampleFormat: JsonFormat[oneChildProtocolExample] = unionFormat1[oneChildProtocolExample, _root_.childRecord]
        |}
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait childRecordFormats {
        |  implicit lazy val childRecordFormat: JsonFormat[childRecord] = new JsonFormat[childRecord] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): childRecord = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          unbuilder.endObject()
        |          new childRecord()
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: childRecord, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.endObject()
        |    }
        |  }
        |}
        |trait Codec { self: _root_.oneChildProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.childRecordFormats => }
        |object Codec extends Codec with _root_.oneChildProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.childRecordFormats""".stripMargin.unindent)
  }

  override def protocolGenerateNested = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val protocol = Interface parse nestedProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait nestedProtocolExampleFormats { self: sjsonnew.BasicJsonProtocol with _root_.nestedProtocolFormats =>
        |  implicit lazy val nestedProtocolExampleFormat: JsonFormat[nestedProtocolExample] = unionFormat1[nestedProtocolExample, _root_.nestedProtocol]
        |}
        |import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait nestedProtocolFormats {
        |  implicit lazy val nestedProtocolFormat: JsonFormat[nestedProtocol] = new JsonFormat[nestedProtocol] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): nestedProtocol = {
        |      deserializationError("No known implementation of nestedProtocol.")
        |    }
        |    override def write[J](obj: nestedProtocol, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of nestedProtocol.")
        |    }
        |  }
        |}
        |trait Codec { self: _root_.nestedProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.nestedProtocolFormats => }
        |object Codec extends Codec with _root_.nestedProtocolExampleFormats with sjsonnew.BasicJsonProtocol with _root_.nestedProtocolFormats""".stripMargin.unindent)
  }

  def protocolGenerateAbstractMethods = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse generateArgDocExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait generateArgDocExampleFormats {
        |  implicit lazy val generateArgDocExampleFormat: JsonFormat[generateArgDocExample] = new JsonFormat[generateArgDocExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): generateArgDocExample = {
        |      deserializationError("No known implementation of generateArgDocExample.")
        |    }
        |    override def write[J](obj: generateArgDocExample, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of generateArgDocExample.")
        |    }
        |  }
        |}
        |trait Codec { self: _root_.generateArgDocExampleFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.generateArgDocExampleFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def recordGenerateSimple = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait simpleRecordExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val simpleRecordExampleFormat: JsonFormat[simpleRecordExample] = new JsonFormat[simpleRecordExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): simpleRecordExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val field = unbuilder.readField[java.net.URL]("field")
        |          unbuilder.endObject()
        |          new simpleRecordExample(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: simpleRecordExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}
        |trait Codec { self: _root_.simpleRecordExampleFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.simpleRecordExampleFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait growableAddOneFieldFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val growableAddOneFieldFormat: JsonFormat[growableAddOneField] = new JsonFormat[growableAddOneField] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): growableAddOneField = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val field = unbuilder.readField[Int]("field")
        |          unbuilder.endObject()
        |          new growableAddOneField(field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: growableAddOneField, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}
        |trait Codec { self: _root_.growableAddOneFieldFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.growableAddOneFieldFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferences = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse primitiveTypesExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait primitiveTypesExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesExampleFormat: JsonFormat[primitiveTypesExample] = new JsonFormat[primitiveTypesExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): primitiveTypesExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val lazyInteger = unbuilder.readField[Int]("lazyInteger")
        |          val arrayInteger = unbuilder.readField[Array[Int]]("arrayInteger")
        |          val lazyArrayInteger = unbuilder.readField[Array[Int]]("lazyArrayInteger")
        |          unbuilder.endObject()
        |          new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, lazyArrayInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |
        |    override def write[J](obj: primitiveTypesExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("lazyInteger", obj.lazyInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.addField("lazyArrayInteger", obj.lazyArrayInteger)
        |      builder.endObject()
        |    }
        |  }
        |}
        |trait Codec { self: _root_.primitiveTypesExampleFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.primitiveTypesExampleFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse primitiveTypesNoLazyExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait primitiveTypesNoLazyExampleFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val primitiveTypesNoLazyExampleFormat: JsonFormat[primitiveTypesNoLazyExample] = new JsonFormat[primitiveTypesNoLazyExample] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): primitiveTypesNoLazyExample = {
        |      jsOpt match {
        |        case Some(js) =>
        |          unbuilder.beginObject(js)
        |          val simpleInteger = unbuilder.readField[Int]("simpleInteger")
        |          val arrayInteger = unbuilder.readField[Array[Int]]("arrayInteger")
        |          unbuilder.endObject()
        |          new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: primitiveTypesNoLazyExample, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("simpleInteger", obj.simpleInteger)
        |      builder.addField("arrayInteger", obj.arrayInteger)
        |      builder.endObject()
        |    }
        |  }
        |}
        |trait Codec { self: _root_.primitiveTypesNoLazyExampleFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.primitiveTypesNoLazyExampleFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(completeExampleCodeCodec.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.withoutEmptyLines must containTheSameElementsAs(completeExampleCodeCodec.withoutEmptyLines)
  }

  def fullCodecCheck = {
    val gen = new CodecCodeGen(genFileName, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)
    val schema = Schema parse s"""{
                                 |  "types": [
                                 |    {
                                 |      "name": "Greeting",
                                 |      "target": "Java",
                                 |      "type": "interface"
                                 |    }
                                 |  ]
                                 |}""".stripMargin
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }
        |trait GreetingFormats {
        |  implicit lazy val GreetingFormat: JsonFormat[Greeting] = new JsonFormat[Greeting] {
        |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Greeting = {
        |      deserializationError("No known implementation of Greeting.")
        |    }
        |    override def write[J](obj: Greeting, builder: Builder[J]): Unit = {
        |      serializationError("No known implementation of Greeting.")
        |    }
        |  }
        |}
        |trait Codec { self: _root_.GreetingFormats with sjsonnew.BasicJsonProtocol => }
        |object Codec extends Codec with _root_.GreetingFormats with sjsonnew.BasicJsonProtocol""".stripMargin.unindent)
  }

}
