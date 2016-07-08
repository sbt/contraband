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
        |sealed abstract class simpleEnumerationExample extends Serializable
        |object simpleEnumerationExample {
        |  /** First type */
        |  case object first extends simpleEnumerationExample
        |
        |  case object second extends simpleEnumerationExample
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val protocol = Interface parse simpleProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """/** example of simple interface */
        |sealed abstract class simpleProtocolExample(
        |  val field: type) extends Serializable {
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
    val protocol = Interface parse oneChildProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """/** example of interface */
        |sealed abstract class oneChildProtocolExample() extends Serializable {
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
        |  private[this] def copy(): childRecord = {
        |    new childRecord()
        |  }
        |}
        |
        |object childRecord {
        |  def apply(): childRecord = new childRecord()
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateNested = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val protocol = Interface parse nestedProtocolExample
    val code = gen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """/** example of nested protocols */
        |sealed abstract class nestedProtocolExample() extends Serializable {
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

  override def protocolGenerateAbstractMethods = {
    val schema = Schema parse generateArgDocExample
    val code = new ScalaCodeGen(genFileName, sealProtocols = false) generate schema

    code.head._2.withoutEmptyLines must containTheSameElementsAs(
      """abstract class generateArgDocExample(
        |  /** I'm a field. */
        |  val field: Int) extends Serializable {
        |  /**
        |   * A very simple example of abstract method.
        |   * Abstract methods can only appear in interface definitions.
        |   * @param arg0 The first argument of the method.
        |                 Make sure it is awesome.
        |   * @param arg1 This argument is not important, so it gets single line doc.
        |   */
        |  def methodExample(arg0: => Array[Int], arg1: Boolean): Array[Int]
        |  override def equals(o: Any): Boolean = o match {
        |    case x: generateArgDocExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "generateArgDocExample(" + field + ")"
        |  }
        |}""".stripMargin.withoutEmptyLines)

  }

  override def recordGenerateSimple = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """/** Example of simple record */
        |final class simpleRecordExample(
        |val field: java.net.URL) extends Serializable {
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
        |  private[this] def copy(field: java.net.URL = field): simpleRecordExample = {
        |    new simpleRecordExample(field)
        |  }
        |  def withField(field: java.net.URL): simpleRecordExample = {
        |    copy(field = field)
        |  }
        |}
        |object simpleRecordExample {
        |  def apply(field: java.net.URL): simpleRecordExample = new simpleRecordExample(field)
        |}""".stripMargin.unindent)
  }

  override def recordGrowZeroToOneField = {
    val gen = new ScalaCodeGen(genFileName, sealProtocols = true)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent must containTheSameElementsAs(
      """final class growableAddOneField(
        |  val field: Int) extends Serializable {
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
        |  private[this] def copy(field: Int = field): growableAddOneField = {
        |    new growableAddOneField(field)
        |  }
        |  def withField(field: Int): growableAddOneField = {
        |    copy(field = field)
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
        |  _lazyArrayInteger: => Array[Int]) extends Serializable {
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
        |  private[this] def copy(simpleInteger: Int = simpleInteger, lazyInteger: => Int = lazyInteger, arrayInteger: Array[Int] = arrayInteger, lazyArrayInteger: => Array[Int] = lazyArrayInteger): primitiveTypesExample = {
        |    new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, lazyArrayInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withLazyInteger(lazyInteger: => Int): primitiveTypesExample = {
        |    copy(lazyInteger = lazyInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Array[Int]): primitiveTypesExample = {
        |    copy(arrayInteger = arrayInteger)
        |  }
        |  def withLazyArrayInteger(lazyArrayInteger: => Array[Int]): primitiveTypesExample = {
        |    copy(lazyArrayInteger = lazyArrayInteger)
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
        |  val arrayInteger: Array[Int]) extends Serializable {
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
        |  private[this] def copy(simpleInteger: Int = simpleInteger, arrayInteger: Array[Int] = arrayInteger): primitiveTypesNoLazyExample = {
        |    new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesNoLazyExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Array[Int]): primitiveTypesNoLazyExample = {
        |    copy(arrayInteger = arrayInteger)
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
