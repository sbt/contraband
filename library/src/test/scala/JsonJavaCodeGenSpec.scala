package sbt.contraband

import java.io.File

import scala.collection.immutable.ListMap
import JsonSchemaExample._

import parser.JsonParser

class JsonJavaCodeGenSpec extends GCodeGenSpec("Java") {
  override def enumerationGenerateSimple = {
    val enumeration = JsonParser.EnumTypeDefinition.parse(simpleEnumerationExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate enumeration

    code.head._2.unindent should equalLines (
      """/** Example of simple enumeration */
        |public enum simpleEnumerationExample {
        |    /** First symbol */
        |    first,
        |    second;
        |    // Some extra code...
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateSimple = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(simpleInterfaceExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate protocol

    code.head._2.unindent should equalLines (
      """/** example of simple interface */
        |public abstract class simpleInterfaceExample implements java.io.Serializable {
        |    // Some extra code...
        |    private type field;
        |    public simpleInterfaceExample(type _field) {
        |        super();
        |        field = _field;
        |    }
        |    public type field() {
        |        return this.field;
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof simpleInterfaceExample)) {
        |            return false;
        |        } else {
        |            simpleInterfaceExample o = (simpleInterfaceExample)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "custom";
        |    }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateOneChild = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(oneChildInterfaceExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate protocol
    val code1 = code.toList(0)._2.unindent
    val code2 = code.toList(1)._2.unindent
    code1 should equalLines (
      """/** example of interface */
        |public abstract class oneChildInterfaceExample implements java.io.Serializable {
        |
        |    private int field;
        |    public oneChildInterfaceExample(int _field) {
        |        super();
        |        field = _field;
        |    }
        |    public int field() {
        |        return this.field;
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof oneChildInterfaceExample)) {
        |            return false;
        |        } else {
        |            oneChildInterfaceExample o = (oneChildInterfaceExample)obj;
        |            return (field() == o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + (new Integer(field())).hashCode());
        |    }
        |    public String toString() {
        |        return "oneChildInterfaceExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
    code2 should equalLines (
      """public final class childRecord extends oneChildInterfaceExample {
        |    private int x;
        |    public childRecord(int _field, int _x) {
        |        super(_field);
        |         x = _x;
        |    }
        |    public int x() {
        |        return this.x;
        |    }
        |    public childRecord withField(int field) {
        |        return new childRecord(field, x);
        |    }
        |    public childRecord withX(int x) {
        |        return new childRecord(field(), x);
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof childRecord)) {
        |            return false;
        |        } else {
        |            childRecord o = (childRecord)obj;
        |            return (field() == o.field()) && (x() == o.x());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (17 + (new Integer(field())).hashCode()) + (new Integer(x())).hashCode());
        |    }
        |    public String toString() {
        |        return "childRecord("  + "field: " + field() + ", " + "x: " + x() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateNested = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(nestedInterfaceExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate protocol

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("nestedProtocolExample.java") ->
          """/** example of nested protocols */
            |public abstract class nestedProtocolExample implements java.io.Serializable {
            |    public nestedProtocolExample() {
            |        super();
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof nestedProtocolExample)) {
            |            return false;
            |        } else {
            |            nestedProtocolExample o = (nestedProtocolExample)obj;
            |            return true;
            |        }
            |    }
            |    public int hashCode() {
            |        return 17;
            |    }
            |    public String toString() {
            |        return "nestedProtocolExample("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("nestedProtocol.java") ->
          """public abstract class nestedProtocol extends nestedProtocolExample {
            |    public nestedProtocol() {
            |        super();
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof nestedProtocol)) {
            |            return false;
            |        } else {
            |            nestedProtocol o = (nestedProtocol)obj;
            |            return true;
            |        }
            |    }
            |    public int hashCode() {
            |        return 17;
            |    }
            |    public String toString() {
            |        return "nestedProtocol("  + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def interfaceGenerateMessages = {
    val schema = JsonParser.Document.parse(generateArgDocExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate schema

    code mapValues (_.withoutEmptyLines) should equalMapLines (
      ListMap(
        new File("generateArgDocExample.java") ->
          """public abstract class generateArgDocExample implements java.io.Serializable {
            |    /** I'm a field. */
            |    private int field;
            |    public generateArgDocExample(int _field) {
            |        super();
            |        field = _field;
            |    }
            |    public int field() {
            |        return this.field;
            |    }
            |    /**
            |     * A very simple example of a message.
            |     * Messages can only appear in interface definitions.
            |     * @param arg0 The first argument of the message.
            |                   Make sure it is awesome.
            |     * @param arg1 This argument is not important, so it gets single line doc.
            |     */
            |    public abstract int[] messageExample(com.example.MyLazy<int[]> arg0,boolean arg1);
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof generateArgDocExample)) {
            |            return false;
            |        } else {
            |            generateArgDocExample o = (generateArgDocExample)obj;
            |            return (field() == o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (17 + (new Integer(field())).hashCode());
            |    }
            |    public String toString() {
            |        return "generateArgDocExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.withoutEmptyLines
      ))
  }

  override def recordGenerateSimple = {
    val record = JsonParser.ObjectTypeDefinition.parse(simpleRecordExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate record

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("simpleRecordExample.java") ->
          """/** Example of simple record */
            |public final class simpleRecordExample implements java.io.Serializable {
            |    // Some extra code...
            |
            |    private java.net.URL field;
            |    public simpleRecordExample(java.net.URL _field) {
            |        super();
            |        field = _field;
            |    }
            |    public java.net.URL field() {
            |        return this.field;
            |    }
            |    public simpleRecordExample withField(java.net.URL field) {
            |        return new simpleRecordExample(field);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof simpleRecordExample)) {
            |            return false;
            |        } else {
            |            simpleRecordExample o = (simpleRecordExample)obj;
            |            return field().equals(o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (17 + field().hashCode());
            |    }
            |    public String toString() {
            |        return "simpleRecordExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def recordGrowZeroToOneField = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableAddOneFieldExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate record

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("growableAddOneField.java") ->
          """public final class growableAddOneField implements java.io.Serializable {
            |    private int field;
            |    public growableAddOneField() {
            |        super();
            |        field = 0;
            |    }
            |    public growableAddOneField(int _field) {
            |        super();
            |        field = _field;
            |    }
            |    public int field() {
            |        return this.field;
            |    }
            |    public growableAddOneField withField(int field) {
            |        return new growableAddOneField(field);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof growableAddOneField)) {
            |            return false;
            |        } else {
            |            growableAddOneField o = (growableAddOneField)obj;
            |            return (field() == o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (17 + (new Integer(field())).hashCode());
            |    }
            |    public String toString() {
            |        return "growableAddOneField("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def recordGrowZeroToOneToTwoFields = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableZeroToOneToTwoFieldsExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate record

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("Foo.java") ->
          """public final class Foo implements java.io.Serializable {
            |    private int x;
            |    private int y;
            |    public Foo() {
            |        super();
            |        x = 0;
            |        y = 0;
            |    }
            |    public Foo(int _x) {
            |        super();
            |        x = _x;
            |        y = 0;
            |    }
            |    public Foo(int _x, int _y) {
            |        super();
            |        x = _x;
            |        y = _y;
            |    }
            |    public int x() {
            |        return this.x;
            |    }
            |    public int y() {
            |        return this.y;
            |    }
            |    public Foo withX(int x) {
            |        return new Foo(x, y);
            |    }
            |    public Foo withY(int y) {
            |        return new Foo(x, y);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof Foo)) {
            |            return false;
            |        } else {
            |            Foo o = (Foo)obj;
            |            return (x() == o.x()) && (y() == o.y());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + (new Integer(x())).hashCode()) + (new Integer(y())).hashCode());
            |    }
            |    public String toString() {
            |        return "Foo("  + "x: " + x() + ", " + "y: " + y() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def schemaGenerateTypeReferences = {
    val schema = JsonParser.Document.parse(primitiveTypesExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate schema

    code.head._2.unindent should equalLines (
      """public final class primitiveTypesExample implements java.io.Serializable {
        |
        |    private int simpleInteger;
        |    private com.example.MyLazy<Integer> lazyInteger;
        |    private int[] arrayInteger;
        |    private com.example.MyOption<Integer> optionInteger;
        |    private com.example.MyLazy<int[]> lazyArrayInteger;
        |    private com.example.MyLazy<com.example.MyOption<Integer>> lazyOptionInteger;
        |    public primitiveTypesExample(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, com.example.MyOption<Integer> _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<com.example.MyOption<Integer>> _lazyOptionInteger) {
        |        super();
        |        simpleInteger = _simpleInteger;
        |        lazyInteger = _lazyInteger;
        |        arrayInteger = _arrayInteger;
        |        optionInteger = _optionInteger;
        |        lazyArrayInteger = _lazyArrayInteger;
        |        lazyOptionInteger = _lazyOptionInteger;
        |    }
        |    public int simpleInteger() {
        |        return this.simpleInteger;
        |    }
        |    public int lazyInteger() {
        |        return this.lazyInteger.get();
        |    }
        |    public int[] arrayInteger() {
        |        return this.arrayInteger;
        |    }
        |    public com.example.MyOption<Integer> optionInteger() {
        |        return this.optionInteger;
        |    }
        |    public int[] lazyArrayInteger() {
        |        return this.lazyArrayInteger.get();
        |    }
        |    public com.example.MyOption<Integer> lazyOptionInteger() {
        |        return this.lazyOptionInteger.get();
        |    }
        |    public primitiveTypesExample withSimpleInteger(int simpleInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyInteger(com.example.MyLazy<Integer> lazyInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withArrayInteger(int[] arrayInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withOptionInteger(com.example.MyOption<Integer> optionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withOptionInteger(int optionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, com.example.MyOption.<Integer>just(optionInteger), lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyArrayInteger(com.example.MyLazy<int[]> lazyArrayInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyOptionInteger(com.example.MyLazy<com.example.MyOption<Integer>> lazyOptionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyOptionInteger(com.example.MyLazy<Integer> lazyOptionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, com.example.MyOption.<Integer>just(lazyOptionInteger));
        |    }
        |    public boolean equals(Object obj) {
        |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
        |    }
        |    public int hashCode() {
        |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
        |    }
        |    public String toString() {
        |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
        |    }
        |}""".stripMargin.unindent)
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val schema = JsonParser.Document.parse(primitiveTypesNoLazyExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate schema

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("primitiveTypesNoLazyExample.java") ->
          """public final class primitiveTypesNoLazyExample implements java.io.Serializable {
            |
            |    private int simpleInteger;
            |
            |    private int[] arrayInteger;
            |    public primitiveTypesNoLazyExample(int _simpleInteger, int[] _arrayInteger) {
            |        super();
            |        simpleInteger = _simpleInteger;
            |        arrayInteger = _arrayInteger;
            |    }
            |    public int simpleInteger() {
            |        return this.simpleInteger;
            |    }
            |    public int[] arrayInteger() {
            |        return this.arrayInteger;
            |    }
            |    public primitiveTypesNoLazyExample withSimpleInteger(int simpleInteger) {
            |        return new primitiveTypesNoLazyExample(simpleInteger, arrayInteger);
            |    }
            |    public primitiveTypesNoLazyExample withArrayInteger(int[] arrayInteger) {
            |        return new primitiveTypesNoLazyExample(simpleInteger, arrayInteger);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof primitiveTypesNoLazyExample)) {
            |            return false;
            |        } else {
            |            primitiveTypesNoLazyExample o = (primitiveTypesNoLazyExample)obj;
            |            return (simpleInteger() == o.simpleInteger()) && java.util.Arrays.equals(arrayInteger(), o.arrayInteger());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + (new Integer(simpleInteger())).hashCode()) + arrayInteger().hashCode());
            |    }
            |    public String toString() {
            |        return "primitiveTypesNoLazyExample("  + "simpleInteger: " + simpleInteger() + ", " + "arrayInteger: " + arrayInteger() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def schemaGenerateComplete = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate schema
    code mapValues (_.unindent) should equalMapLines (completeExampleCodeJava mapValues (_.unindent))
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional) generate schema
    code mapValues (_.withoutEmptyLines) should equalMapLines (completeExampleCodeJava mapValues (_.withoutEmptyLines))
  }

  lazy val instantiateJavaOptional: (String, String) => String =
    {
      (tpe: String, e: String) =>
        e match {
          case "null" => s"com.example.MyOption.<$tpe>nothing()"
          case e      => s"com.example.MyOption.<$tpe>just($e)"
        }
    }
}
