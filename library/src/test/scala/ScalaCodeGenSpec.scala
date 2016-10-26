package sbt.datatype

import java.io.File

import NewSchema._

class ScalaCodeGenSpec extends GCodeGenSpec("Scala") {

  val outputFile = new File("output.scala")
  val scalaArray = "Vector"
  val genFileName = (_: Definition) => outputFile

  override def enumerationGenerateSimple = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = gen generate enumeration

    code.head._2.unindent should contain theSameElementsAs
      """/** Example of simple enumeration */
        |sealed abstract class simpleEnumerationExample extends Serializable
        |object simpleEnumerationExample {
        |  // Some extra code...
        |  /** First symbol */
        |  case object first extends simpleEnumerationExample
        |
        |  case object second extends simpleEnumerationExample
        |}""".stripMargin.unindent
  }

  override def interfaceGenerateSimple = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val protocol = Interface parse simpleInterfaceExample
    val code = gen generate protocol

    code.head._2.unindent should contain theSameElementsAs
      """/** example of simple interface */
        |sealed abstract class simpleInterfaceExample(
        |  val field: type) extends Serializable {
        |  // Some extra code...
        |  override def equals(o: Any): Boolean = o match {
        |    case x: simpleInterfaceExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "simpleInterfaceExample(" + field + ")"
        |  }
        |}""".stripMargin.unindent
  }

  override def interfaceGenerateOneChild = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val protocol = Interface parse oneChildInterfaceExample
    val code = gen generate protocol

    code.head._2.unindent should contain theSameElementsAs
      """/** example of interface */
        |sealed abstract class oneChildInterfaceExample(
        |    val field: Int) extends Serializable {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: oneChildInterfaceExample => (this.field == x.field)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (17 + field.##)
        |  }
        |  override def toString: String = {
        |    "oneChildInterfaceExample(" + field + ")"
        |  }
        |}
        |final class childRecord(
        |  field: Int,
        |  val x: Int) extends oneChildInterfaceExample(field) {
        |  override def equals(o: Any): Boolean = o match {
        |    case x: childRecord => (this.field == x.field) && (this.x == x.x)
        |    case _ => false
        |  }
        |  override def hashCode: Int = {
        |    37 * (37 * (17 + field.##) + x.##)
        |  }
        |  override def toString: String = {
        |    "childRecord(" + field + ", " + x + ")"
        |  }
        |  private[this] def copy(field: Int = field, x: Int = x): childRecord = {
        |    new childRecord(field, x)
        |  }
        |  def withField(field: Int): childRecord = {
        |    copy(field = field)
        |  }
        |  def withX(x: Int): childRecord = {
        |    copy(x = x)
        |  }
        |}
        |
        |object childRecord {
        |  def apply(field: Int, x: Int): childRecord = new childRecord(field, x)
        |}""".stripMargin.unindent
  }

  override def interfaceGenerateNested = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val protocol = Interface parse nestedInterfaceExample
    val code = gen generate protocol

    code.head._2.unindent should contain theSameElementsAs
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
        |    "nestedProtocolExample()"
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
        |    "nestedProtocol()"
        |  }
        |}""".stripMargin.unindent
  }

  override def interfaceGenerateMessages = {
    val schema = Schema parse generateArgDocExample
    val code = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = false) generate schema

    code.head._2.withoutEmptyLines should contain theSameElementsAs
      """abstract class generateArgDocExample(
        |  /** I'm a field. */
        |  val field: Int) extends Serializable {
        |  /**
        |   * A very simple example of a message.
        |   * Messages can only appear in interface definitions.
        |   * @param arg0 The first argument of the message.
        |                 Make sure it is awesome.
        |   * @param arg1 This argument is not important, so it gets single line doc.
        |   */
        |  def messageExample(arg0: => Vector[Int], arg1: Boolean): Vector[Int]
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
        |}""".stripMargin.withoutEmptyLines

  }

  override def recordGenerateSimple = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val record = Record parse simpleRecordExample
    val code = gen generate record

    code.head._2.unindent should contain theSameElementsAs
      """/** Example of simple record */
        |final class simpleRecordExample(
        |val field: java.net.URL) extends Serializable {
        |  // Some extra code...
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
        |}""".stripMargin.unindent
  }

  override def recordGrowZeroToOneField = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val record = Record parse growableAddOneFieldExample
    val code = gen generate record

    code.head._2.unindent should contain theSameElementsAs
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
        |}""".stripMargin.unindent
  }

  override def recordGrowZeroToOneToTwoFields = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val record = Record parse growableZeroToOneToTwoFieldsExample
    val code = gen generate record

    val obtained = code.head._2.unindent
    val expected =
      """final class Foo(
        |  val x: Int,
        |  val y: Int) extends Serializable {
        |  def this() = this(0, 0)
        |  def this(x: Int) = this(x, 0)
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
        |  private[this] def copy(x: Int = x, y: Int = y): Foo = {
        |    new Foo(x, y)
        |  }
        |  def withX(x: Int): Foo = {
        |    copy(x = x)
        |  }
        |  def withY(y: Int): Foo = {
        |    copy(y = y)
        |  }
        |}
        |object Foo {
        |  def apply(): Foo = new Foo(0, 0)
        |  def apply(x: Int): Foo = new Foo(x, 0)
        |  def apply(x: Int, y: Int): Foo = new Foo(x, y)
        |}""".stripMargin.unindent
    TestUtils.printUnifiedDiff(expected, obtained)
    obtained should contain theSameElementsAs expected
  }

  override def schemaGenerateTypeReferences = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val schema = Schema parse primitiveTypesExample
    val code = gen generate schema
    code.head._2.unindent should contain theSameElementsAs
      """final class primitiveTypesExample(
        |  val simpleInteger: Int,
        |  _lazyInteger: => Int,
        |  val arrayInteger: Vector[Int],
        |  val optionInteger: Option[Int],
        |  _lazyArrayInteger: => Vector[Int],
        |  _lazyOptionInteger: => Option[Int]) extends Serializable {
        |
        |
        |  lazy val lazyInteger: Int = _lazyInteger
        |  lazy val lazyArrayInteger: Vector[Int] = _lazyArrayInteger
        |  lazy val lazyOptionInteger: Option[Int] = _lazyOptionInteger
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
        |  private[this] def copy(simpleInteger: Int = simpleInteger, lazyInteger: => Int = lazyInteger, arrayInteger: Vector[Int] = arrayInteger, optionInteger: Option[Int] = optionInteger, lazyArrayInteger: => Vector[Int] = lazyArrayInteger, lazyOptionInteger: => Option[Int] = lazyOptionInteger): primitiveTypesExample = {
        |    new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withLazyInteger(lazyInteger: => Int): primitiveTypesExample = {
        |    copy(lazyInteger = lazyInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Vector[Int]): primitiveTypesExample = {
        |    copy(arrayInteger = arrayInteger)
        |  }
        |  def withOptionInteger(optionInteger: Option[Int]): primitiveTypesExample = {
        |    copy(optionInteger = optionInteger)
        |  }
        |  def withLazyArrayInteger(lazyArrayInteger: => Vector[Int]): primitiveTypesExample = {
        |    copy(lazyArrayInteger = lazyArrayInteger)
        |  }
        |  def withLazyOptionInteger(lazyOptionInteger: => Option[Int]): primitiveTypesExample = {
        |    copy(lazyOptionInteger = lazyOptionInteger)
        |  }
        |}
        |
        |object primitiveTypesExample {
        |  def apply(simpleInteger: Int, lazyInteger: => Int, arrayInteger: Vector[Int], optionInteger: Option[Int], lazyArrayInteger: => Vector[Int], lazyOptionInteger: => Option[Int]): primitiveTypesExample = new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger)
        |}""".stripMargin.unindent
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val schema = Schema parse primitiveTypesNoLazyExample
    val code = gen generate schema

    code.head._2.unindent should contain theSameElementsAs
      """final class primitiveTypesNoLazyExample(
        |
        |  val simpleInteger: Int,
        |
        |  val arrayInteger: Vector[Int]) extends Serializable {
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
        |  private[this] def copy(simpleInteger: Int = simpleInteger, arrayInteger: Vector[Int] = arrayInteger): primitiveTypesNoLazyExample = {
        |    new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |  }
        |  def withSimpleInteger(simpleInteger: Int): primitiveTypesNoLazyExample = {
        |    copy(simpleInteger = simpleInteger)
        |  }
        |  def withArrayInteger(arrayInteger: Vector[Int]): primitiveTypesNoLazyExample = {
        |    copy(arrayInteger = arrayInteger)
        |  }
        |}
        |
        |object primitiveTypesNoLazyExample {
        |  def apply(simpleInteger: Int, arrayInteger: Vector[Int]): primitiveTypesNoLazyExample = new primitiveTypesNoLazyExample(simpleInteger, arrayInteger)
        |}""".stripMargin.unindent
  }

  override def schemaGenerateComplete = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val schema = Schema parse completeExample
    val code = gen generate schema
    // println(code.toString)
    code.head._2.unindent should contain theSameElementsAs completeExampleCodeScala.unindent
  }

  override def schemaGenerateCompletePlusIndent = {
    val gen = new ScalaCodeGen(scalaArray, genFileName, sealProtocols = true)
    val schema = Schema parse completeExample
    val code = gen generate schema

    code.head._2.withoutEmptyLines should contain theSameElementsAs completeExampleCodeScala.withoutEmptyLines
  }

}
