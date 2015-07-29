package sbt.datatype

import java.io.File

import org.specs2._
import NewSchema._

class ScalaCodeGenSpec extends GCodeGenSpec("Scala") {

  val outputFile = new File("output.scala")
  val genFileName = (_: Definition) => outputFile

  override def enumerationGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
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
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val protocol = Protocol parse simpleProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
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
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val protocol = Protocol parse oneChildProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
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
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val protocol = Protocol parse nestedProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
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
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
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

  override def recordGrowZeroToOneField = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """final class growableAddOneField(
        |  val field: Int)  {
        |  def this() = this(0)
        |  override def equals(o: Any): Boolean = o match {
        |    case x: growableAddOneField => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "growableAddOneField(" + field + ")"
        |  }
        |}
        |object growableAddOneField {
        |  def apply(): growableAddOneField = new growableAddOneField(0)
        |  def apply(field: Int): growableAddOneField = new growableAddOneField(field)
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferences = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val schema = Schema parse primitiveTypesExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """final class primitiveTypesExample(
        |
        |  val simpleInteger: Int,
        |  _lazyInteger: => Int,
        |
        |  val arrayInteger: Array[Int],
        |  _lazyArrayInteger: => Array[Int])  {
        |
        |
        |  lazy val lazyInteger: Int = _lazyInteger
        |
        |  lazy val lazyArrayInteger: Array[Int] = _lazyArrayInteger
        |  override def equals(o: Any): Boolean = o match {
        |    case x: primitiveTypesExample => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
        |  }
        |  override def toString: String = {
        |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
        |  }
        |}
        |
        |object primitiveTypesExample {
        |  def apply(simpleInteger: Int, lazyInteger: => Int, arrayInteger: Array[Int], lazyArrayInteger: => Array[Int]): primitiveTypesExample = new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, lazyArrayInteger)
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val schema = Schema parse primitiveTypesNoLazyExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(
      """final class primitiveTypesNoLazyExample(
        |
        |  val simpleInteger: Int,
        |
        |  val arrayInteger: Array[Int])  {
        |
        |
        |  override def equals(o: Any): Boolean = o match {
        |    case x: primitiveTypesNoLazyExample => (this.simpleInteger == x.simpleInteger) && (this.arrayInteger == x.arrayInteger)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + simpleInteger.##) + arrayInteger.##)
        |  }
        |  override def toString: String = {
        |    "primitiveTypesNoLazyExample(" + simpleInteger + ", " + arrayInteger + ")"
        |  }
        |}
        |
        |object primitiveTypesNoLazyExample {
        |  def apply(simpleInteger: Int, arrayInteger: Array[Int]): primitiveTypesNoLazyExample = new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateComplete = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.unindent must containTheSameElementsAs(completeExampleCodeScala.unindent)
  }

  override def schemaGenerateCompletePlusIndent = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.withoutEmptyLines must containTheSameElementsAs(completeExampleCodeScala.withoutEmptyLines)
  }

}
