package sbt.datatype

import org.specs2._
import NewSchema._

class ScalaCodeGenSpec extends Specification {

  implicit class CleanedString(s: String) {
    def unindent: List[String] = s.lines.toList map (_.trim) filterNot (_.isEmpty)
    def withoutEmptyLines: List[String] = s.lines.toList filterNot (_.trim.isEmpty)
  }

  val outputFileName = "output.scala"
  val genFileName = (_: Definition) => outputFileName

  def is = s2"""
    This is a specification for the generation of Scala code.

    generate(Enumeration) should
      generate a simple enumeration              $enumerationGenerateSimple

    generate(Protocol) should
      generate a simple protocol                 $protocolGenerateSimple
      generate a simple protocol with one child  $protocolGenerateOneChild
      generate nested protocols                  $protocolGenerateNested

    generate(Record) should
      generate a simple record                   $recordGenerateSimple

    generate(Schema) should
      generate a complete schema                 $schemaGenerateComplete
      generate and indent a complete schema      $schemaGenerateCompletePlusIndent
  """

  def enumerationGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code(outputFileName).unindent must containTheSameElementsAs(
      """/** Example of simple enumeration */
        |sealed abstract class simpleEnumerationExample
        |object simpleEnumerationExample {
        |  /** First type */
        |  case object first extends simpleEnumerationExample
        |
        |  case object second extends simpleEnumerationExample
        |}""".stripMargin.unindent)
  }

  def protocolGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse simpleProtocolExample
    val code = gen.generate(protocol, None, Nil)

    code(outputFileName).unindent must containTheSameElementsAs(
      """/** example of simple protocol */
        |sealed abstract class simpleProtocolExample(
        |  val field: type)  {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: simpleProtocolExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "simpleProtocolExample(" + field + ")"
        |  }
        |}""".stripMargin.unindent)
  }

  def protocolGenerateOneChild = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse oneChildProtocolExample
    val code = gen.generate(protocol, None, Nil)

    code(outputFileName).unindent must containTheSameElementsAs(
      """/** example of protocol */
        |sealed abstract class oneChildProtocolExample()  {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: oneChildProtocolExample => true
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    17
        |  }
        |  override def toString: String = {
        |    "oneChildProtocolExample(" +  + ")"
        |  }
        |}
        |final class childRecord() extends oneChildProtocolExample() {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: childRecord => true
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    17
        |  }
        |  override def toString: String = {
        |    "childRecord(" +  + ")"
        |  }
        |}
        |
        |object childRecord {
        |  def apply(): childRecord = new childRecord()
        |}""".stripMargin.unindent)
  }

  def protocolGenerateNested = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse nestedProtocolExample
    val code = gen.generate(protocol, None, Nil)

    code(outputFileName).unindent must containTheSameElementsAs(
      """/** example of nested protocols */
        |sealed abstract class nestedProtocolExample()  {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: nestedProtocolExample => true
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    17
        |  }
        |  override def toString: String = {
        |    "nestedProtocolExample(" +  + ")"
        |  }
        |}
        |sealed abstract class nestedProtocol() extends nestedProtocolExample() {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: nestedProtocol => true
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    17
        |  }
        |  override def toString: String = {
        |    "nestedProtocol(" +  + ")"
        |  }
        |}""".stripMargin.unindent)
  }

  def recordGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName)
    val record = Record parse simpleRecordExample
    val code = gen.generate(record, None, Nil)

    code(outputFileName).unindent must containTheSameElementsAs(
      """/** Example of simple record */
        |final class simpleRecordExample(
        |val field: type)  {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: simpleRecordExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "simpleRecordExample(" + field + ")"
        |  }
        |}
        |object simpleRecordExample {
        |  def apply(field: type): simpleRecordExample = new simpleRecordExample(field)
        |}""".stripMargin.unindent)
  }

  def schemaGenerateComplete = {
    val gen = new ScalaCodeGen(genFileName)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code(outputFileName).unindent must containTheSameElementsAs(completeExampleCode.unindent)
  }

  def schemaGenerateCompletePlusIndent = {
    val gen = new ScalaCodeGen(genFileName)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code(outputFileName).withoutEmptyLines must containTheSameElementsAs(completeExampleCode.withoutEmptyLines)
  }

}
