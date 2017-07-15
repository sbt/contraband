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
    val code = mkJavaCodeGen.generate(ast)
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
    val code = mkJavaCodeGen.generate(ast)
    code.head._2.unindent should equalLines(
      """package com.example;
        |/** Example of a type */
        |public final class TypeExample implements java.io.Serializable {
        |    // Some extra code
        |
        |    public static TypeExample create(java.util.Optional<java.net.URL> _field) {
        |        return new TypeExample(_field);
        |    }
        |    public static TypeExample of(java.util.Optional<java.net.URL> _field) {
        |        return new TypeExample(_field);
        |    }
        |    public static TypeExample create(java.net.URL _field) {
        |        return new TypeExample(_field);
        |    }
        |    public static TypeExample of(java.net.URL _field) {
        |        return new TypeExample(_field);
        |    }
        |    private java.util.Optional<java.net.URL> field;
        |    protected TypeExample(java.util.Optional<java.net.URL> _field) {
        |        super();
        |        field = _field;
        |    }
        |    protected TypeExample(java.net.URL _field) {
        |        super();
        |        field = java.util.Optional.<java.net.URL>ofNullable(_field);
        |    }
        |    public java.util.Optional<java.net.URL> field() {
        |        return this.field;
        |    }
        |    public TypeExample withField(java.util.Optional<java.net.URL> field) {
        |        return new TypeExample(field);
        |    }
        |    public TypeExample withField(java.net.URL field) {
        |        return new TypeExample(java.util.Optional.<java.net.URL>ofNullable(field));
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
        |        return 37 * (37 * (17 + "com.example.TypeExample".hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "TypeExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 1 field" in {
    val Success(ast) = SchemaParser.parse(growableAddOneFieldExample)
    // println(ast)
    val code = mkJavaCodeGen.generate(ast)

    code.head._2.unindent should equalLines (
      """package com.example;
        |public final class Growable implements java.io.Serializable {
        |    public static Growable create() {
        |        return new Growable();
        |    }
        |    public static Growable of() {
        |        return new Growable();
        |    }
        |    public static Growable create(java.util.Optional<Integer> _field) {
        |        return new Growable(_field);
        |    }
        |    public static Growable of(java.util.Optional<Integer> _field) {
        |        return new Growable(_field);
        |    }
        |    public static Growable create(int _field) {
        |        return new Growable(_field);
        |    }
        |    public static Growable of(int _field) {
        |        return new Growable(_field);
        |    }
        |    private java.util.Optional<Integer> field;
        |    protected Growable() {
        |        super();
        |        field = java.util.Optional.<Integer>ofNullable(0);
        |    }
        |    protected Growable(java.util.Optional<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    protected Growable(int _field) {
        |        super();
        |        field = java.util.Optional.<Integer>ofNullable(_field);
        |    }
        |    public java.util.Optional<Integer> field() {
        |        return this.field;
        |    }
        |    public Growable withField(java.util.Optional<Integer> field) {
        |        return new Growable(field);
        |    }
        |    public Growable withField(int field) {
        |        return new Growable(java.util.Optional.<Integer>ofNullable(field));
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
        |        return 37 * (37 * (17 + "com.example.Growable".hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "Growable("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "grow a record from 0 to 2 field" in {
    val Success(ast) = SchemaParser.parse(growableZeroToOneToTwoFieldsExample)
    // println(ast)
    val code = mkJavaCodeGen.generate(ast)

    code.head._2.unindent should equalLines (
      """package com.example;
        |public final class Foo implements java.io.Serializable {
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
        |        x = java.util.Optional.<Integer>empty();
        |        y = new Array {};
        |    }
        |    protected Foo(java.util.Optional<Integer> _x) {
        |        super();
        |        x = _x;
        |        y = new Array {};
        |    }
        |    protected Foo(int _x) {
        |        super();
        |        x = java.util.Optional.<Integer>ofNullable(_x);
        |        y = new Array {};
        |    }
        |    protected Foo(java.util.Optional<Integer> _x, int[] _y) {
        |        super();
        |        x = _x;
        |        y = _y;
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
        |            return x().equals(o.x()) && java.util.Arrays.equals(y(), o.y());
        |        }
        |    }
        |    public int hashCode() {
        |        return 37 * (37 * (37 * (17 + "com.example.Foo".hashCode()) + x().hashCode()) + y().hashCode());
        |    }
        |    public String toString() {
        |        return "Foo("  + "x: " + x() + ", " + "y: " + y() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  "generate(Interface)" should "generate an interface with one child" in {
    val Success(ast) = SchemaParser.parse(intfExample)
    val code = mkJavaCodeGen.generate(ast)

    val code1 = code.toList(0)._2.unindent
    val code2 = code.toList(1)._2.unindent
    code1 should equalLines (
      """package com.example;
        |/** Example of an interface */
        |public abstract class InterfaceExample implements java.io.Serializable {
        |
        |    // Some extra code
        |    private java.util.Optional<Integer> field;
        |    protected InterfaceExample(java.util.Optional<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    protected InterfaceExample(int _field) {
        |        super();
        |        field = java.util.Optional.<Integer>ofNullable(_field);
        |    }
        |    public java.util.Optional<Integer> field() {
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
        |        return 37 * (37 * (17 + "com.example.InterfaceExample".hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "InterfaceExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
    code2 should equalLines (
      """package com.example;
        |public final class ChildType extends com.example.InterfaceExample {
        |    public static ChildType create(java.util.Optional<String> _name, java.util.Optional<Integer> _field) {
        |        return new ChildType(_name, _field);
        |    }
        |    public static ChildType of(java.util.Optional<String> _name, java.util.Optional<Integer> _field) {
        |        return new ChildType(_name, _field);
        |    }
        |    public static ChildType create(String _name, int _field) {
        |        return new ChildType(_name, _field);
        |    }
        |    public static ChildType of(String _name, int _field) {
        |        return new ChildType(_name, _field);
        |    }
        |    private java.util.Optional<String> name;
        |    protected ChildType(java.util.Optional<String> _name, java.util.Optional<Integer> _field) {
        |        super(_field);
        |         name = _name;
        |    }
        |    protected ChildType(String _name, int _field) {
        |        super(java.util.Optional.<Integer>ofNullable(_field));
        |         name = java.util.Optional.<String>ofNullable(_name);
        |    }
        |    public java.util.Optional<String> name() {
        |        return this.name;
        |    }
        |    public ChildType withName(java.util.Optional<String> name) {
        |        return new ChildType(name, field());
        |    }
        |    public ChildType withName(String name) {
        |        return new ChildType(java.util.Optional.<String>ofNullable(name), field());
        |    }
        |    public ChildType withField(java.util.Optional<Integer> field) {
        |        return new ChildType(name, field);
        |    }
        |    public ChildType withField(int field) {
        |        return new ChildType(name, java.util.Optional.<Integer>ofNullable(field));
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
        |        return 37 * (37 * (37 * (17 + "com.example.ChildType".hashCode()) + name().hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "ChildType("  + "name: " + name() + ", " + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent)
  }

  it should "generate messages" in {
    val Success(ast) = SchemaParser.parse(messageExample)
    val code = mkJavaCodeGen.generate(ast)
    code.head._2.unindent should equalLines(
      """package com.example;
        |public abstract class IntfExample implements java.io.Serializable {
        |    /** I'm a field. */
        |    private java.util.Optional<Integer> field;
        |    protected IntfExample(java.util.Optional<Integer> _field) {
        |        super();
        |        field = _field;
        |    }
        |    protected IntfExample(int _field) {
        |        super();
        |        field = java.util.Optional.<Integer>ofNullable(_field);
        |    }
        |    public java.util.Optional<Integer> field() {
        |        return this.field;
        |    }
        |    /**
        |     * A very simple example of a message.
        |     * Messages can only appear in interface definitions.
        |     * @param arg0 The first argument of the message.
        |                   Make sure it is awesome.
        |     * @param arg1 This argument is not important, so it gets single line doc.
        |     */
        |    public abstract int[] messageExample(com.example.MyLazy<int[]> arg0,java.util.Optional<Boolean> arg1);
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
        |        return 37 * (37 * (17 + "com.example.IntfExample".hashCode()) + field().hashCode());
        |    }
        |    public String toString() {
        |        return "IntfExample("  + "field: " + field() + ")";
        |    }
        |}""".stripMargin.unindent
    )
  }
  def mkJavaCodeGen: JavaCodeGen =
    new JavaCodeGen("com.example.MyLazy", CodeGen.javaOptional, CodeGen.instantiateJavaOptional,
        wrapOption = true)
}
