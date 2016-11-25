package sbt.contraband

import org.scalatest._
import java.io.File
import parser.SchemaParser
import GraphQLExample._
import scala.util.Success

class GraphQLJavaCodeGenSpec extends FlatSpec with Matchers with Inside with EqualLines {
  "generate(Enumeration)" should "generate a simple enumeration" in {
    val Success(ast) = SchemaParser.parse(simpleEnumerationExample)
    // println(ast)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines (
      """package com.example;
        |/** Example of an enumeration */
        |public enum EnumExample {
        |    /** First symbol */
        |    First,
        |    Second;
        |    // Some extra code
        |}""".stripMargin.unindent)
  }

  "generate(Record)" should "generate a record" in {
    val Success(ast) = SchemaParser.parse(recordExample)
    // println(ast)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example;
        |/** Example of a type */
        |public final class TypeExample implements java.io.Serializable {
        |    // Some extra code
        |
        |    private com.example.MyOption<java.net.URL> field;
        |    public TypeExample(com.example.MyOption<java.net.URL> _field) {
        |        super();
        |        field = _field;
        |    }
        |    public com.example.MyOption<java.net.URL> field() {
        |        return this.field;
        |    }
        |    public TypeExample withField(com.example.MyOption<java.net.URL> field) {
        |        return new TypeExample(field);
        |    }
        |    public TypeExample withField(java.net.URL field) {
        |        return new TypeExample(com.example.MyOption.<java.net.URL>just(field));
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof TypeExample)) {
        |            return false;
        |        } else {
        |            TypeExample o = (TypeExample)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "TypeExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 1 field" in {
    val Success(ast) = SchemaParser.parse(growableAddOneFieldExample)
    // println(ast)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)

    code.head._2.unindent should equalLines (
      """package com.example;
        |public final class Growable implements java.io.Serializable {
        |    private com.example.MyOption<Integer> field;
        |    public Growable() {
        |        super();
        |        field = com.example.MyOption.<Integer>just(0);
        |    }
        |    public Growable(com.example.MyOption<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    public com.example.MyOption<Integer> field() {
        |        return this.field;
        |    }
        |    public Growable withField(com.example.MyOption<Integer> field) {
        |        return new Growable(field);
        |    }
        |    public Growable withField(int field) {
        |        return new Growable(com.example.MyOption.<Integer>just(field));
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof Growable)) {
        |            return false;
        |        } else {
        |            Growable o = (Growable)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "Growable("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 2 field" in {
    val Success(ast) = SchemaParser.parse(growableZeroToOneToTwoFieldsExample)
    // println(ast)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)

    code.head._2.unindent should equalLines (
      """package com.example;
        |public final class Foo implements java.io.Serializable {
        |    private com.example.MyOption<Integer> x;
        |    private int[] y;
        |    public Foo() {
        |        super();
        |        x = com.example.MyOption.<Integer>nothing();
        |        y = new Array {};
        |    }
        |    public Foo(com.example.MyOption<Integer> _x) {
        |        super();
        |        x = _x;
        |        y = new Array {};
        |    }
        |    public Foo(com.example.MyOption<Integer> _x, int[] _y) {
        |        super();
        |        x = _x;
        |        y = _y;
        |    }
        |    public com.example.MyOption<Integer> x() {
        |        return this.x;
        |    }
        |    public int[] y() {
        |        return this.y;
        |    }
        |    public Foo withX(com.example.MyOption<Integer> x) {
        |        return new Foo(x, y);
        |    }
        |    public Foo withX(int x) {
        |        return new Foo(com.example.MyOption.<Integer>just(x), y);
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
        |            return x().equals(o.x()) && java.util.Arrays.equals(y(), o.y());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (17 + x().hashCode()) + y().hashCode());
        |    }
        |    public String toString() {
        |        return "Foo("  + "x: " + x() + ", " + "y: " + y() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  "generate(Interface)" should "generate an interface with one child" in {
    val Success(ast) = SchemaParser.parse(intfExample)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)

    val code1 = code.toList(0)._2.unindent
    val code2 = code.toList(1)._2.unindent
    code1 should equalLines (
      """package com.example;
        |/** Example of an interface */
        |public abstract class InterfaceExample implements java.io.Serializable {
        |
        |    // Some extra code
        |    private com.example.MyOption<Integer> field;
        |    public InterfaceExample(com.example.MyOption<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    public com.example.MyOption<Integer> field() {
        |        return this.field;
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof InterfaceExample)) {
        |            return false;
        |        } else {
        |            InterfaceExample o = (InterfaceExample)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "InterfaceExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
    code2 should equalLines (
      """package com.example;
        |public final class ChildType extends com.example.InterfaceExample {
        |    private com.example.MyOption<String> name;
        |    public ChildType(com.example.MyOption<String> _name, com.example.MyOption<Integer> _field) {
        |        super(_field);
        |         name = _name;
        |    }
        |    public com.example.MyOption<String> name() {
        |        return this.name;
        |    }
        |    public ChildType withName(com.example.MyOption<String> name) {
        |        return new ChildType(name, field());
        |    }
        |    public ChildType withName(String name) {
        |        return new ChildType(com.example.MyOption.<String>just(name), field());
        |    }
        |    public ChildType withField(com.example.MyOption<Integer> field) {
        |        return new ChildType(name, field);
        |    }
        |    public ChildType withField(int field) {
        |        return new ChildType(name, com.example.MyOption.<Integer>just(field));
        |    }
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof ChildType)) {
        |            return false;
        |        } else {
        |            ChildType o = (ChildType)obj;
        |            return name().equals(o.name()) && field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (17 + name().hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "ChildType("  + "name: " + name() + ", " + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "generate messages" in {
    val Success(ast) = SchemaParser.parse(messageExample)
    val gen = new JavaCodeGen("com.example.MyLazy", "com.example.MyOption", instantiateJavaOptional)
    val code = gen generate Transform.propateNamespace(ast)
    code.head._2.unindent should equalLines(
      """package com.example;
        |public abstract class IntfExample implements java.io.Serializable {
        |    /** I'm a field. */
        |    private com.example.MyOption<Integer> field;
        |    public IntfExample(com.example.MyOption<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    public com.example.MyOption<Integer> field() {
        |        return this.field;
        |    }
        |    /**
        |     * A very simple example of a message.
        |     * Messages can only appear in interface definitions.
        |     * @param arg0 The first argument of the message.
        |                   Make sure it is awesome.
        |     * @param arg1 This argument is not important, so it gets single line doc.
        |     */
        |    public abstract int[] messageExample(com.example.MyLazy<int[]> arg0,com.example.MyOption<Boolean> arg1);
        |    public boolean equals(Object obj) {
        |        if (this == obj) {
        |            return true;
        |        } else if (!(obj instanceof IntfExample)) {
        |            return false;
        |        } else {
        |            IntfExample o = (IntfExample)obj;
        |            return field().equals(o.field());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (17 + field().hashCode());
        |    }
        |    public String toString() {
        |        return "IntfExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent
    )
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
