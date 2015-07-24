package sbt.datatype

import org.specs2._
import NewSchema._

class ScalaCodeGenSpec extends GCodeGenSpec("Scala") {

  val outputFileName = "output.scala"
  val genFileName = (_: Definition) => outputFileName

  override def enumerationGenerateSimple = {
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

  override def protocolGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse simpleProtocolExample
    val code = gen generate protocol

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

  override def protocolGenerateOneChild = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse oneChildProtocolExample
    val code = gen generate protocol

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

  override def protocolGenerateNested = {
    val gen = new ScalaCodeGen(genFileName)
    val protocol = Protocol parse nestedProtocolExample
    val code = gen generate protocol

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

  override def recordGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName)
    val record = Record parse simpleRecordExample
    val code = gen generate record

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

  override def schemaGenerateComplete = {
    val gen = new ScalaCodeGen(genFileName)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code(outputFileName).unindent must containTheSameElementsAs(completeExampleCodeScala.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val gen = new ScalaCodeGen(genFileName)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code(outputFileName).withoutEmptyLines must containTheSameElementsAs(completeExampleCodeScala.withoutEmptyLines)
  }

}
