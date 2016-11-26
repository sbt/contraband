package sbt.contraband

import org.scalatest._
import java.io.File
import parser.SchemaParser
import GraphQLExample._
import scala.util.Success

class GraphQLScalaCodeGenSpec extends FlatSpec with Matchers with Inside with EqualLines {
  "generate(Enumeration)" should "generate a simple enumeration" in {
    val Success(ast) = SchemaParser.parse(simpleEnumerationExample)
    // println(ast)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example
        |/** Example of an enumeration */
        |sealed abstract class EnumExample extends Serializable
        |object EnumExample {
        |  // Some extra code
        |  /** First symbol */
        |  case object First extends EnumExample
        |
        |  case object Second extends EnumExample
        |}""".stripMargin.unindent)
  }

  "generate(Record)" should "generate a record" in {
    val Success(ast) = SchemaParser.parse(recordExample)
    // println(ast)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example
        |/** Example of a type */
        |final class TypeExample(
        |val field: Option[java.net.URL]) extends Serializable {
        |  // Some extra code
        |  override def equals(o: Any): Boolean = o match {
        |    case x: TypeExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "TypeExample(" + field + ")"
        |  }
        |  protected[this] def copy(field: Option[java.net.URL] = field): TypeExample = {
        |    new TypeExample(field)
        |  }
        |  def withField(field: Option[java.net.URL]): TypeExample = {
        |    copy(field = field)
        |  }
        |  def withField(field: java.net.URL): TypeExample = {
        |    copy(field = Option(field))
        |  }
        |}
        |object TypeExample {
        |  def apply(field: Option[java.net.URL]): TypeExample = new TypeExample(field)
        |}""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 1 field" in {
    val Success(ast) = SchemaParser.parse(growableAddOneFieldExample)
    // println(ast)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)

    code.head._2.unindent should equalLines (
      """package com.example
        |final class Growable(
        |  val field: Option[Int]) extends Serializable {
        |  def this() = this(Option(0))
        |  override def equals(o: Any): Boolean = o match {
        |    case x: Growable => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "Growable(" + field + ")"
        |  }
        |  protected[this] def copy(field: Option[Int] = field): Growable = {
        |    new Growable(field)
        |  }
        |  def withField(field: Option[Int]): Growable = {
        |    copy(field = field)
        |  }
        |  def withField(field: Int): Growable = {
        |    copy(field = Option(field))
        |  }
        |}
        |object Growable {
        |  def apply(): Growable = new Growable(Option(0))
        |  def apply(field: Option[Int]): Growable = new Growable(field)
        |}
        |""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 2 field" in {
    val Success(ast) = SchemaParser.parse(growableZeroToOneToTwoFieldsExample)
    // println(ast)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)

    code.head._2.unindent should equalLines (
      """package com.example
        |final class Foo(
        |  val x: Option[Int],
        |  val y: Vector[Int]) extends Serializable {
        |  def this() = this(None, Vector())
        |  def this(x: Option[Int]) = this(x, Vector())
        |  override def equals(o: Any): Boolean = o match {
        |    case x: Foo => (this.x == x.x) && (this.y == x.y)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + x.##) + y.##)
        |  }
        |  override def toString: String = {
        |    "Foo(" + x + ", " + y + ")"
        |  }
        |  protected[this] def copy(x: Option[Int] = x, y: Vector[Int] = y): Foo = {
        |    new Foo(x, y)
        |  }
        |  def withX(x: Option[Int]): Foo = {
        |    copy(x = x)
        |  }
        |  def withX(x: Int): Foo = {
        |    copy(x = Option(x))
        |  }
        |  def withY(y: Vector[Int]): Foo = {
        |    copy(y = y)
        |  }
        |}
        |object Foo {
        |  def apply(): Foo = new Foo(None, Vector())
        |  def apply(x: Option[Int]): Foo = new Foo(x, Vector())
        |  def apply(x: Option[Int], y: Vector[Int]): Foo = new Foo(x, y)
        |}
        |""".stripMargin.unindent)
  }

  "generate(Interface)" should "generate an interface with one child" in {
    val Success(ast) = SchemaParser.parse(intfExample)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example
        |/** Example of an interface */
        |sealed abstract class InterfaceExample(
        |val field: Option[Int]) extends Serializable {
        |  // Some extra code
        |  override def equals(o: Any): Boolean = o match {
        |    case x: InterfaceExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "InterfaceExample(" + field + ")"
        |  }
        |}
        |object InterfaceExample {
        |}
        |final class ChildType(
        |  val name: Option[String],
        |  field: Option[Int]) extends com.example.InterfaceExample(field) with Serializable {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: ChildType => (this.name == x.name) && (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + name.##) + field.##)
        |  }
        |  override def toString: String = {
        |    "ChildType(" + name + ", " + field + ")"
        |  }
        |  protected[this] def copy(name: Option[String] = name, field: Option[Int] = field): ChildType = {
        |    new ChildType(name, field)
        |  }
        |  def withName(name: Option[String]): ChildType = {
        |    copy(name = name)
        |  }
        |  def withName(name: String): ChildType = {
        |    copy(name = Option(name))
        |  }
        |  def withField(field: Option[Int]): ChildType = {
        |    copy(field = field)
        |  }
        |  def withField(field: Int): ChildType = {
        |    copy(field = Option(field))
        |  }
        |}
        |
        |object ChildType {
        |  def apply(name: Option[String], field: Option[Int]): ChildType = new ChildType(name, field)
        |}""".stripMargin.unindent)
  }

  it should "generate messages" in {
    val Success(ast) = SchemaParser.parse(messageExample)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example
        |sealed abstract class IntfExample(
        |  /** I'm a field. */
        |  val field: Option[Int]) extends Serializable {
        |  /**
        |   * A very simple example of a message.
        |   * Messages can only appear in interface definitions.
        |   * @param arg0 The first argument of the message.
        |                 Make sure it is awesome.
        |   * @param arg1 This argument is not important, so it gets single line doc.
        |   */
        |  def messageExample(arg0: => Vector[Int], arg1: Option[Boolean]): Vector[Int]
        |  override def equals(o: Any): Boolean = o match {
        |    case x: IntfExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "IntfExample(" + field + ")"
        |  }
        |}
        |
        |object IntfExample {
        |}
        |""".stripMargin.unindent
    )
  }

  it should "generate with customization" in {
    val Success(ast) = SchemaParser.parse(customizationExample)
    val code = mkScalaCodeGen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example
        |/** Example of an interface */
        |sealed abstract class IntfExample(
        |  val field: Option[Int]) extends Interface1 with Interface2 with Serializable {
        |  // Some extra code...
        |  override def equals(o: Any): Boolean = o match {
        |    case x: IntfExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    return "custom";
        |  }
        |}
        |
        |object IntfExample extends CompanionInterface1 with CompanionInterface2 {
        |  // Some extra companion code...
        |}
        |""".stripMargin.unindent
    )
  }

  def mkScalaCodeGen: ScalaCodeGen =
    new ScalaCodeGen(javaLazy, javaOptional, instantiateJavaOptional, scalaArray, genFileName, sealProtocols = true)
  lazy val instantiateJavaOptional: (String, String) => String =
    {
      (tpe: String, e: String) =>
        e match {
          case "null" => s"com.example.Maybe.<$tpe>nothing()"
          case e      => s"com.example.Maybe.<$tpe>just($e)"
        }
    }
  val javaLazy = "com.example.Lazy"
  val javaOptional = "com.example.Maybe"
  val outputFile = new File("output.scala")
  val scalaArray = "Vector"
  val genFileName = (_: Any) => outputFile
}
