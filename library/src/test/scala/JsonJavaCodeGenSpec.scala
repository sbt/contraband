package sbt.contraband

import java.io.File

import scala.collection.immutable.ListMap
import JsonSchemaExample._

import parser.JsonParser

class JsonJavaCodeGenSpec extends GCodeGenSpec("Java") {
  override def enumerationGenerateSimple = {
    val enumeration = JsonParser.EnumTypeDefinition.parse(simpleEnumerationExample)
    val code = mkJavaCodeGen generate enumeration

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
    val code = mkJavaCodeGen generate protocol

    code.head._2.unindent should equalLines (
      """/** example of simple interface */
        |public abstract class simpleInterfaceExample implements java.io.Serializable {
        |    // Some extra code...
        |    private type field;
        |    protected simpleInterfaceExample(type _field) {
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
        |            return this.field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (17 + "simpleInterfaceExample".hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "custom";
        |    }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateOneChild = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(oneChildInterfaceExample)
    val code = mkJavaCodeGen generate protocol
    val code1 = code.toList(0)._2.unindent
    val code2 = code.toList(1)._2.unindent
    code1 should equalLines (
      """/** example of interface */
        |public abstract class oneChildInterfaceExample implements java.io.Serializable {
        |
        |    private int field;
        |    protected oneChildInterfaceExample(int _field) {
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
        |            return (this.field() == o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (17 + "oneChildInterfaceExample".hashCode()) + Integer.valueOf(field()).hashCode());
        |    }
        |    public String toString() {
        |        return "oneChildInterfaceExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
    code2 should equalLines (
      """public final class childRecord extends oneChildInterfaceExample {
        |    public static childRecord create(int _field, int _x) {
        |        return new childRecord(_field, _x);
        |    }
        |    public static childRecord of(int _field, int _x) {
        |        return new childRecord(_field, _x);
        |    }
        |    private int x;
        |    protected childRecord(int _field, int _x) {
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
        |            return (this.field() == o.field()) && (this.x() == o.x());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (37 * (17 + "childRecord".hashCode()) + Integer.valueOf(field()).hashCode()) + Integer.valueOf(x()).hashCode());
        |    }
        |    public String toString() {
        |        return "childRecord("  + "field: " + field() + ", " + "x: " + x() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  override def interfaceGenerateNested = {
    val protocol = JsonParser.InterfaceTypeDefinition.parseInterface(nestedInterfaceExample)
    val code = mkJavaCodeGen generate protocol

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("nestedProtocolExample.java") ->
          """/** example of nested protocols */
            |public abstract class nestedProtocolExample implements java.io.Serializable {
            |    protected nestedProtocolExample() {
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
            |        return 37 * (17 + "nestedProtocolExample".hashCode());
            |    }
            |    public String toString() {
            |        return "nestedProtocolExample("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("nestedProtocol.java") ->
          """public abstract class nestedProtocol extends nestedProtocolExample {
            |    protected nestedProtocol() {
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
            |        return 37 * (17 + "nestedProtocol".hashCode());
            |    }
            |    public String toString() {
            |        return "nestedProtocol("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("ChildRecord.java") ->
          """public final class ChildRecord extends nestedProtocol {
            |    public static ChildRecord create() {
            |        return new ChildRecord();
            |    }
            |    public static ChildRecord of() {
            |        return new ChildRecord();
            |    }
            |    protected ChildRecord() {
            |        super();
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof ChildRecord)) {
            |            return false;
            |        } else {
            |            ChildRecord o = (ChildRecord)obj;
            |            return true;
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (17 + "ChildRecord".hashCode());
            |    }
            |    public String toString() {
            |        return "ChildRecord("  + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def interfaceGenerateMessages = {
    val schema = JsonParser.Document.parse(generateArgDocExample)
    val code = mkJavaCodeGen generate schema

    code.mapValues(_.withoutEmptyLines).toMap should equalMapLines (
      ListMap(
        new File("generateArgDocExample.java") ->
          """public abstract class generateArgDocExample implements java.io.Serializable {
            |    private int field;
            |    protected generateArgDocExample(int _field) {
            |        super();
            |        field = _field;
            |    }
            |    /** I'm a field. */
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
            |            return (this.field() == o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + "generateArgDocExample".hashCode()) + Integer.valueOf(field()).hashCode());
            |    }
            |    public String toString() {
            |        return "generateArgDocExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.withoutEmptyLines
      ))
  }

  override def recordGenerateSimple = {
    val record = JsonParser.ObjectTypeDefinition.parse(simpleRecordExample)
    val code = mkJavaCodeGen generate record

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("simpleRecordExample.java") ->
          """/** Example of simple record */
            |public final class simpleRecordExample implements java.io.Serializable {
            |    // Some extra code...
            |
            |    public static simpleRecordExample create(java.net.URL _field) {
            |        return new simpleRecordExample(_field);
            |    }
            |    public static simpleRecordExample of(java.net.URL _field) {
            |        return new simpleRecordExample(_field);
            |    }
            |    private java.net.URL field;
            |    protected simpleRecordExample(java.net.URL _field) {
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
            |            return this.field().equals(o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + "simpleRecordExample".hashCode()) + field().hashCode());
            |    }
            |    public String toString() {
            |        return "simpleRecordExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def recordGrowZeroToOneField = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableAddOneFieldExample)
    val code = mkJavaCodeGen generate record

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("growableAddOneField.java") ->
          """public final class growableAddOneField implements java.io.Serializable {
            |    public static growableAddOneField create() {
            |        return new growableAddOneField();
            |    }
            |    public static growableAddOneField of() {
            |        return new growableAddOneField();
            |    }
            |    public static growableAddOneField create(int _field) {
            |        return new growableAddOneField(_field);
            |    }
            |    public static growableAddOneField of(int _field) {
            |        return new growableAddOneField(_field);
            |    }
            |    private int field;
            |    protected growableAddOneField() {
            |        super();
            |        field = 0;
            |    }
            |    protected growableAddOneField(int _field) {
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
            |            return (this.field() == o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + "growableAddOneField".hashCode()) + Integer.valueOf(field()).hashCode());
            |    }
            |    public String toString() {
            |        return "growableAddOneField("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def recordGrowZeroToOneToTwoFields = {
    val record = JsonParser.ObjectTypeDefinition.parse(growableZeroToOneToTwoFieldsJavaExample)
    val code = mkJavaCodeGen generate record

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("Foo.java") ->
          """public final class Foo implements java.io.Serializable {
            |    public static Foo create() {
            |        return new Foo();
            |    }
            |    public static Foo of() {
            |        return new Foo();
            |    }
            |    public static Foo create(java.util.Optional<Integer> _x) {
            |        return new Foo(_x);
            |    }
            |    public static Foo of(java.util.Optional<Integer> _x) {
            |        return new Foo(_x);
            |    }
            |    public static Foo create(int _x) {
            |        return new Foo(_x);
            |    }
            |    public static Foo of(int _x) {
            |        return new Foo(_x);
            |    }
            |    public static Foo create(java.util.Optional<Integer> _x, int[] _y) {
            |        return new Foo(_x, _y);
            |    }
            |    public static Foo of(java.util.Optional<Integer> _x, int[] _y) {
            |        return new Foo(_x, _y);
            |    }
            |    public static Foo create(int _x, int[] _y) {
            |        return new Foo(_x, _y);
            |    }
            |    public static Foo of(int _x, int[] _y) {
            |        return new Foo(_x, _y);
            |    }
            |    private java.util.Optional<Integer> x;
            |    private int[] y;
            |    protected Foo() {
            |        super();
            |        x = java.util.Optional.<String>ofNullable(0);
            |        y = new Array { 0 };
            |    }
            |    protected Foo(java.util.Optional<Integer> _x) {
            |        super();
            |        x = _x;
            |        y = new Array { 0 };
            |    }
            |    protected Foo(int _x) {
            |        super();
            |        x = java.util.Optional.<Integer>ofNullable(_x);
            |        y = new Array { 0 };
            |    }
            |    protected Foo(java.util.Optional<Integer> _x, int[] _y) {
            |      super();
            |      x = _x;
            |      y = _y;
            |    }
            |    protected Foo(int _x, int[] _y) {
            |        super();
            |        x = java.util.Optional.<Integer>ofNullable(_x);
            |        y = _y;
            |    }
            |    public java.util.Optional<Integer> x() {
            |        return this.x;
            |    }
            |    public int[] y() {
            |        return this.y;
            |    }
            |    public Foo withX(java.util.Optional<Integer> x) {
            |        return new Foo(x, y);
            |    }
            |    public Foo withX(int x) {
            |        return new Foo(java.util.Optional.<Integer>ofNullable(x), y);
            |    }
            |    public Foo withY(int[] y) {
            |        return new Foo(x, y);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof Foo)) {
            |            return false;
            |        } else {
            |            Foo o = (Foo)obj;
            |            return this.x().equals(o.x()) && java.util.Arrays.equals(this.y(), o.y());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (37 * (17 + "Foo".hashCode()) + x().hashCode()) + java.util.Arrays.hashCode(y()));
            |    }
            |    public String toString() {
            |        return "Foo("  + "x: " + x() + ", " + "y: " + y() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def recordPrimitives: Unit = {
    val record = JsonParser.ObjectTypeDefinition.parse(primitiveTypesExample2)
    val code = mkJavaCodeGen generate record

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("primitiveTypesExample2.java") ->
          """public final class primitiveTypesExample2 implements java.io.Serializable {


    public static primitiveTypesExample2 create(boolean _smallBoolean, boolean _bigBoolean) {
        return new primitiveTypesExample2(_smallBoolean, _bigBoolean);
    }
    public static primitiveTypesExample2 of(boolean _smallBoolean, boolean _bigBoolean) {
        return new primitiveTypesExample2(_smallBoolean, _bigBoolean);
    }
    private boolean smallBoolean;
    private boolean bigBoolean;
    protected primitiveTypesExample2(boolean _smallBoolean, boolean _bigBoolean) {
        super();
        smallBoolean = _smallBoolean;
        bigBoolean = _bigBoolean;
    }
    public boolean smallBoolean() {
        return this.smallBoolean;
    }
    public boolean bigBoolean() {
        return this.bigBoolean;
    }
    public primitiveTypesExample2 withSmallBoolean(boolean smallBoolean) {
        return new primitiveTypesExample2(smallBoolean, bigBoolean);
    }
    public primitiveTypesExample2 withBigBoolean(boolean bigBoolean) {
        return new primitiveTypesExample2(smallBoolean, bigBoolean);
    }
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof primitiveTypesExample2)) {
            return false;
        } else {
            primitiveTypesExample2 o = (primitiveTypesExample2)obj;
            return (this.smallBoolean() == o.smallBoolean()) && (this.bigBoolean() == o.bigBoolean());
        }
    }
    public int hashCode() {
        return 37 * (37 * (37 * (17 + "primitiveTypesExample2".hashCode()) + Boolean.valueOf(smallBoolean()).hashCode()) + Boolean.valueOf(bigBoolean()).hashCode());
    }
    public String toString() {
        return "primitiveTypesExample2("  + "smallBoolean: " + smallBoolean() + ", " + "bigBoolean: " + bigBoolean() + ")";
    }
}""".stripMargin.unindent
      ))
  }

  override def recordWithModifier: Unit = {
    val record = JsonParser.ObjectTypeDefinition.parse(modifierExample)
    val code = mkJavaCodeGen generate record

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("modifierExample.java") ->
          """sealed class modifierExample implements java.io.Serializable {
            |
            |    public static modifierExample create(int _field) {
            |        return new modifierExample(_field);
            |    }
            |    public static modifierExample of(int _field) {
            |        return new modifierExample(_field);
            |    }
            |    private int field;
            |    protected modifierExample(int _field) {
            |        super();
            |        field = _field;
            |    }
            |    public int field() {
            |        return this.field;
            |    }
            |    public modifierExample withField(int field) {
            |        return new modifierExample(field);
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof modifierExample)) {
            |            return false;
            |        } else {
            |            modifierExample o = (modifierExample)obj;
            |            return (this.field() == o.field());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (17 + "modifierExample".hashCode()) + Integer.valueOf(field()).hashCode());
            |    }
            |    public String toString() {
            |        return "modifierExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def schemaGenerateTypeReferences = {
    val schema = JsonParser.Document.parse(primitiveTypesExample)
    val code = mkJavaCodeGen generate schema

    code.head._2.unindent should equalLines (
      """public final class primitiveTypesExample implements java.io.Serializable {
        |
        |    public static primitiveTypesExample create(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, java.util.Optional<Integer> _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        return new primitiveTypesExample(_simpleInteger, _lazyInteger, _arrayInteger, _optionInteger, _lazyArrayInteger, _lazyOptionInteger);
        |    }
        |    public static primitiveTypesExample of(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, java.util.Optional<Integer> _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        return new primitiveTypesExample(_simpleInteger, _lazyInteger, _arrayInteger, _optionInteger, _lazyArrayInteger, _lazyOptionInteger);
        |    }
        |    public static primitiveTypesExample create(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, int _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        return new primitiveTypesExample(_simpleInteger, _lazyInteger, _arrayInteger, _optionInteger, _lazyArrayInteger, _lazyOptionInteger);
        |    }
        |    public static primitiveTypesExample of(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, int _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        return new primitiveTypesExample(_simpleInteger, _lazyInteger, _arrayInteger, _optionInteger, _lazyArrayInteger, _lazyOptionInteger);
        |    }
        |    private int simpleInteger;
        |    private com.example.MyLazy<Integer> lazyInteger;
        |    private int[] arrayInteger;
        |    private java.util.Optional<Integer> optionInteger;
        |    private com.example.MyLazy<int[]> lazyArrayInteger;
        |    private com.example.MyLazy<java.util.Optional<Integer>> lazyOptionInteger;
        |    protected primitiveTypesExample(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, java.util.Optional<Integer> _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        super();
        |        simpleInteger = _simpleInteger;
        |        lazyInteger = _lazyInteger;
        |        arrayInteger = _arrayInteger;
        |        optionInteger = _optionInteger;
        |        lazyArrayInteger = _lazyArrayInteger;
        |        lazyOptionInteger = _lazyOptionInteger;
        |    }
        |    protected primitiveTypesExample(int _simpleInteger, com.example.MyLazy<Integer> _lazyInteger, int[] _arrayInteger, int _optionInteger, com.example.MyLazy<int[]> _lazyArrayInteger, com.example.MyLazy<java.util.Optional<Integer>> _lazyOptionInteger) {
        |        super();
        |        simpleInteger = _simpleInteger;
        |        lazyInteger = _lazyInteger;
        |        arrayInteger = _arrayInteger;
        |        optionInteger = java.util.Optional.<Integer>ofNullable(_optionInteger);
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
        |    public java.util.Optional<Integer> optionInteger() {
        |        return this.optionInteger;
        |    }
        |    public int[] lazyArrayInteger() {
        |        return this.lazyArrayInteger.get();
        |    }
        |    public java.util.Optional<Integer> lazyOptionInteger() {
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
        |    public primitiveTypesExample withOptionInteger(java.util.Optional<Integer> optionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withOptionInteger(int optionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, java.util.Optional.<Integer>ofNullable(optionInteger), lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyArrayInteger(com.example.MyLazy<int[]> lazyArrayInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyOptionInteger(com.example.MyLazy<java.util.Optional<Integer>> lazyOptionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyOptionInteger(com.example.MyLazy<Integer> lazyOptionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, java.util.Optional.<Integer>ofNullable(lazyOptionInteger));
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
    val code = mkJavaCodeGen generate schema

    code.mapValues(_.unindent).toMap should equalMapLines (
      ListMap(
        new File("primitiveTypesNoLazyExample.java") ->
          """public final class primitiveTypesNoLazyExample implements java.io.Serializable {
            |
            |    public static primitiveTypesNoLazyExample create(int _simpleInteger, int[] _arrayInteger) {
            |        return new primitiveTypesNoLazyExample(_simpleInteger, _arrayInteger);
            |    }
            |    public static primitiveTypesNoLazyExample of(int _simpleInteger, int[] _arrayInteger) {
            |        return new primitiveTypesNoLazyExample(_simpleInteger, _arrayInteger);
            |    }
            |    private int simpleInteger;
            |
            |    private int[] arrayInteger;
            |    protected primitiveTypesNoLazyExample(int _simpleInteger, int[] _arrayInteger) {
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
            |            return (this.simpleInteger() == o.simpleInteger()) && java.util.Arrays.equals(this.arrayInteger(), o.arrayInteger());
            |        }
            |    }
            |    public int hashCode() {
            |        return 37 * (37 * (37 * (17 + "primitiveTypesNoLazyExample".hashCode()) + Integer.valueOf(simpleInteger()).hashCode()) + java.util.Arrays.hashCode(arrayInteger()));
            |    }
            |    public String toString() {
            |        return "primitiveTypesNoLazyExample("  + "simpleInteger: " + simpleInteger() + ", " + "arrayInteger: " + arrayInteger() + ")";
            |    }
            |}""".stripMargin.unindent
      ))
  }

  override def schemaGenerateComplete = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = mkJavaCodeGen generate schema
    code.mapValues(_.unindent).toMap should equalMapLines (completeExampleCodeJava.mapValues(_.unindent).toMap)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = JsonParser.Document.parse(completeExample)
    val code = mkJavaCodeGen generate schema
    code.mapValues(_.withoutEmptyLines).toMap should equalMapLines (completeExampleCodeJava.mapValues(_.withoutEmptyLines).toMap)
  }

  def mkJavaCodeGen: JavaCodeGen =
    new JavaCodeGen("com.example.MyLazy", CodeGen.javaOptional, CodeGen.instantiateJavaOptional,
        wrapOption = true)
}
