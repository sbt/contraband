package sbt.datatype

import java.io.File

import org.specs2._
import NewSchema._

class JavaCodeGenSpec extends GCodeGenSpec("Java") {

  override def enumerationGenerateSimple = {
    val enumeration = Enumeration parse simpleEnumerationExample
    val code = JavaCodeGen generate enumeration

    code.head._2.unindent must containTheSameElementsAs(
      """/** Example of simple enumeration */
        |public enum simpleEnumerationExample {
        |    /** First type */
        |    first,
        |    second
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateSimple = {
    val protocol = Protocol parse simpleProtocolExample
    val code = JavaCodeGen generate protocol

    code.head._2.unindent must containTheSameElementsAs(
      """/** example of simple protocol */
        |public abstract class simpleProtocolExample  {
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
        |        return  "simpleProtocolExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  override def protocolGenerateOneChild = {
    val protocol = Protocol parse oneChildProtocolExample
    val code = JavaCodeGen generate protocol

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
        new File("oneChildProtocolExample.java") ->
          """/** example of protocol */
            |public abstract class oneChildProtocolExample  {
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
            |        return  "oneChildProtocolExample("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("childRecord.java") ->
          """public final class childRecord extends oneChildProtocolExample {
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
            |        return  "childRecord("  + ")";
            |    }
            |}""".stripMargin.unindent
      ).toList
    )
  }

  override def protocolGenerateNested = {
    val protocol = Protocol parse nestedProtocolExample
    val code = JavaCodeGen generate protocol

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
        new File("nestedProtocolExample.java") ->
          """/** example of nested protocols */
            |public abstract class nestedProtocolExample  {
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
            |        return  "nestedProtocolExample("  + ")";
            |    }
            |}""".stripMargin.unindent,

        new File("nestedProtocol.java") ->
          """public abstract class nestedProtocol extends nestedProtocolExample {
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
            |        return  "nestedProtocol("  + ")";
            |    }
            |}""".stripMargin.unindent
      ).toList
    )
  }

  override def recordGenerateSimple = {
    val record = Record parse simpleRecordExample
    val code = JavaCodeGen generate record

    code mapValues (_.unindent) must containTheSameElementsAs(
      Map(
        new File("simpleRecordExample.java") ->
          """/** Example of simple record */
            |public final class simpleRecordExample  {
            |
            |    private type field;
            |    public simpleRecordExample(type _field) {
            |        super();
            |        field = _field;
            |    }
            |    public type field() {
            |        return this.field;
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
            |        return  "simpleRecordExample("  + "field: " + field() + ")";
            |    }
            |}""".stripMargin.unindent
      ).toList
    )
  }

  override def schemaGenerateComplete = {
    val schema = Schema parse completeExample
    val code = JavaCodeGen generate schema

    code mapValues (_.unindent) must containTheSameElementsAs(completeExampleCodeJava mapValues (_.unindent) toList)
  }

  override def schemaGenerateCompletePlusIndent = {
    val schema = Schema parse completeExample
    val code = JavaCodeGen generate schema

    code mapValues (_.withoutEmptyLines) must containTheSameElementsAs(completeExampleCodeJava mapValues (_.withoutEmptyLines) toList)
  }

}
