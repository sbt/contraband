package sbt.contraband

import org.scalatest._
import java.io.File
import parser.SchemaParser
import GraphQLExample._
import scala.util.Success

class GraphQLCodecCodeGenSpec extends FlatSpec with Matchers with Inside with EqualLines {
  "generate(Interface)" should "generate a codec for an interface" in {
    val Success(ast) = SchemaParser.parse(intfExample)
    val code = mkCodecCodeGen.generate(ast)

    code(new File("generated", "InterfaceExampleFormats.scala")).unindent should equalLines (
      """/**
        | * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait InterfaceExampleFormats { self: sjsonnew.BasicJsonProtocol with generated.ChildTypeFormats =>
        |  implicit lazy val InterfaceExampleFormat: JsonFormat[com.example.InterfaceExample] = flatUnionFormat1[com.example.InterfaceExample, com.example.ChildType]("type")
        |}""".stripMargin.unindent)
    code(new File("generated", "ChildTypeFormats.scala")).unindent should equalLines (
      """/**
        | * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait ChildTypeFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val ChildTypeFormat: JsonFormat[com.example.ChildType] = new JsonFormat[com.example.ChildType] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.ChildType = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val name = unbuilder.readField[Option[String]]("name")
        |          val field = unbuilder.readField[Option[Int]]("field")
        |          unbuilder.endObject()
        |          com.example.ChildType(name, field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: com.example.ChildType, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("name", obj.name)
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  it should "generate a codec for a two-level hierarchy of interfaces" in {
    val Success(ast) = SchemaParser.parse(twoLevelIntfExample)
    val code = mkCodecCodeGen.generate(ast)

    code(new File("generated", "InterfaceExampleFormats.scala")).unindent should equalLines (
      """/**
        | * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.JsonFormat
        |
        |trait InterfaceExampleFormats { self: sjsonnew.BasicJsonProtocol with generated.ChildTypeFormats =>
        |  implicit lazy val InterfaceExampleFormat: JsonFormat[com.example.InterfaceExample] = flatUnionFormat1[com.example.InterfaceExample, com.example.ChildType]("type")
        |}""".stripMargin.unindent)
    code.contains(new File("generated", "MiddleInterfaceFormats.scala")) shouldEqual false
    code(new File("generated", "ChildTypeFormats.scala")).unindent should equalLines (
      """/**
        | * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
        | */
        |
        |// DO NOT EDIT MANUALLY
        |package generated
        |
        |import _root_.sjsonnew.{ Unbuilder, Builder, JsonFormat, deserializationError }
        |
        |trait ChildTypeFormats { self: sjsonnew.BasicJsonProtocol =>
        |  implicit lazy val ChildTypeFormat: JsonFormat[com.example.ChildType] = new JsonFormat[com.example.ChildType] {
        |    override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): com.example.ChildType = {
        |      __jsOpt match {
        |        case Some(__js) =>
        |          unbuilder.beginObject(__js)
        |          val name = unbuilder.readField[Option[String]]("name")
        |          val field = unbuilder.readField[Option[Int]]("field")
        |          unbuilder.endObject()
        |          com.example.ChildType(name, field)
        |        case None =>
        |          deserializationError("Expected JsObject but found None")
        |      }
        |    }
        |    override def write[J](obj: com.example.ChildType, builder: Builder[J]): Unit = {
        |      builder.beginObject()
        |      builder.addField("name", obj.name)
        |      builder.addField("field", obj.field)
        |      builder.endObject()
        |    }
        |  }
        |}""".stripMargin.unindent)
  }

  val codecParents = List("sjsonnew.BasicJsonProtocol")
  val instantiateJavaLazy = (s: String) => s"mkLazy($s)"
  val javaOption = "com.example.Option"
  val scalaArray = "Vector"
  val formatsForType: ast.Type => List[String] = CodecCodeGen.formatsForType
  def mkCodecCodeGen: CodecCodeGen =
    new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, Nil)
}
