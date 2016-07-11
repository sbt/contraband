package sbt.datatype

import java.io.File

import org.specs2._
import NewSchema._

class JavaCodeGenSpec extends GCodeGenSpec("Java") {

  override def enumerationGenerateSimple = {
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
      """/** Example of simple enumeration */
        |public enum simpleEnumerationExample {
        |    /** First type */
        |    first,
        |    second
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateSimple = {
    val protocol = Interface parse simpleProtocolExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """/** example of simple interface */
        |public abstract class simpleProtocolExample implements java.io.Serializable {
        |    private type field;
        |    public simpleProtocolExample(type _field) {
        |        super();
        |        field = _field;
        |    }
        |    public type field() {
        |        return this.field;
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof simpleProtocolExample)) {
        |            return false;
        |        } else {
        |            simpleProtocolExample o = (simpleProtocolExample)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "simpleProtocolExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateOneChild = {
    val protocol = Interface parse oneChildProtocolExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate protocol

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
        new File("oneChildProtocolExample.java") ->
          """/** example of interface */
            |public abstract class oneChildProtocolExample implements java.io.Serializable {
            |    public oneChildProtocolExample() {
            |        super();
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |        return true;
            |        } else if (!(obj instanceof oneChildProtocolExample)) {
            |            return false;
            |        } else {
            |            oneChildProtocolExample o = (oneChildProtocolExample)obj;
            |            return true;
            |        }
            |    }
            |    public int hashCode() {
            |        return 17;
            |    }
            |    public String toString() {
            |        return "oneChildProtocolExample("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("childRecord.java") ->
          """public final class childRecord extends oneChildProtocolExample {
            |    public childRecord() {
            |        super();
            |    }
            |    public boolean equals(Object obj) {
            |        if (this == obj) {
            |            return true;
            |        } else if (!(obj instanceof childRecord)) {
            |            return false;
            |        } else {
            |            childRecord o = (childRecord)obj;
            |            return true;
            |        }
            |    }
            |    public int hashCode() {
            |        return 17;
            |    }
            |    public String toString() {
            |        return "childRecord("  + ")";
            |    }
            |}""".stripMargin.unindent
      ).toList
    )
  }

  override def protocolGenerateNested = {
    val protocol = Interface parse nestedProtocolExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate protocol

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
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
      ).toList
    )
  }

  override def protocolGenerateAbstractMethods = {
    val schema = Schema parse generateArgDocExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate schema

    code mapValues (_.withoutEmptyLines) must containTheSameElementsAs(
      Map(
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
            |     * A very simple example of abstract method.
            |     * Abstract methods can only appear in interface definitions.
            |     * @param arg0 The first argument of the method.
            |                   Make sure it is awesome.
            |     * @param arg1 This argument is not important, so it gets single line doc.
            |     */
            |    public abstract int[] methodExample(com.example.MyLazy<int[]> arg0,boolean arg1);
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
      ).toList
    )
  }

  override def recordGenerateSimple = {
    val record = Record parse simpleRecordExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate record

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
        new File("simpleRecordExample.java") ->
          """/** Example of simple record */
            |public final class simpleRecordExample implements java.io.Serializable {
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
      ).toList
    )
  }

  override def recordGrowZeroToOneField = {
    val record = Record parse growableAddOneFieldExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate record

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
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
      ).toList
    )
  }

  override def schemaGenerateTypeReferences = {
    val schema = Schema parse primitiveTypesExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate schema

    code.head._2.unindent must containTheSameElementsAs(
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
        |    public int optionInteger() {
        |        return this.optionInteger;
        |    }
        |    public int[] lazyArrayInteger() {
        |        return this.lazyArrayInteger.get();
        |    }
        |    public int lazyOptionInteger() {
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
        |    public primitiveTypesExample withLazyArrayInteger(com.example.MyLazy<int[]> lazyArrayInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
        |    }
        |    public primitiveTypesExample withLazyOptionInteger(com.example.MyLazy<com.example.MyOption<Integer>> lazyOptionInteger) {
        |        return new primitiveTypesExample(simpleInteger, lazyInteger, arrayInteger, optionInteger, lazyArrayInteger, lazyOptionInteger);
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
        |}""".stripMargin.unindent
    )
  }

  override def schemaGenerateTypeReferencesNoLazy = {
    val schema = Schema parse primitiveTypesNoLazyExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate schema

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
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
      ).toList
    )
  }

  override def schemaGenerateComplete = {
    val schema = Schema parse completeExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate schema

    code mapValues (_.unindent) must containTheSameElementsAs(completeExampleCodeJava mapValues (_.unindent) toList)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = Schema parse completeExample
    val code = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption") generate schema

    code mapValues (_.withoutEmptyLines) must containTheSameElementsAs(completeExampleCodeJava mapValues (_.withoutEmptyLines) toList)
  }

}
