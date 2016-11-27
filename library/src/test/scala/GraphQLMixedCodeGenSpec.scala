package sbt.contraband

import org.scalatest._
import java.io.File
import parser.SchemaParser
import GraphQLExample._
import scala.util.Success
import scala.collection.immutable._

class GraphQLMixedCodeGenSpec extends FlatSpec with Matchers with Inside with EqualLines {
  "generate(Record)" should "handle mixed Java-Scala inheritance" in {
    val Success(ast) = SchemaParser.parse(mixedExample)
    // println(ast)
    val gen = new MixedCodeGen(javaLazy, javaOptional, instantiateJavaOptional,
      scalaArray, genFileName, scalaSealProtocols = true, scalaPrivateConstructor = true,
      wrapOption = true)
    val code = gen.generate(ast)

    code mapValues (_.unindent) should equalMapLines (
      ListMap(
        new File("com/example/Greeting.java") ->
          """/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package com.example;
public abstract class Greeting implements java.io.Serializable {


    private String message;
    private com.example.Maybe<Integer> number;
    public Greeting(String _message) {
        super();
        message = _message;
        number = com.example.Maybe.<Integer>nothing();
    }
    public Greeting(String _message, com.example.Maybe<Integer> _number) {
        super();
        message = _message;
        number = _number;
    }
    public Greeting(String _message, int _number) {
        super();
        message = _message;
        number = com.example.Maybe.<Integer>just(_number);
    }
    public String message() {
        return this.message;
    }
    public com.example.Maybe<Integer> number() {
        return this.number;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Greeting)) {
            return false;
        } else {
            Greeting o = (Greeting)obj;
            return message().equals(o.message()) && number().equals(o.number());
        }
    }
    public int hashCode() {
        return 37 * (37 * (17 + message().hashCode()) + number().hashCode());
    }
    public String toString() {
        return "Greeting("  + "message: " + message() + ", " + "number: " + number() + ")";
    }
}
""".stripMargin.unindent,
        new File("output.scala") ->
          """/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package com.example
final class SimpleGreeting private (
  message: String,
  number: com.example.Maybe[java.lang.Integer]) extends com.example.Greeting(message, number) with Serializable {
  private def this(message: String) = this(message, com.example.Maybe.nothing[java.lang.Integer]())
  override def equals(o: Any): Boolean = o match {
    case x: SimpleGreeting => (this.message == x.message) && (this.number == x.number)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + message.##) + number.##)
  }
  override def toString: String = {
    "SimpleGreeting(" + message + ", " + number + ")"
  }
  protected[this] def copy(message: String = message, number: com.example.Maybe[java.lang.Integer] = number): SimpleGreeting = {
    new SimpleGreeting(message, number)
  }
  def withMessage(message: String): SimpleGreeting = {
    copy(message = message)
  }
  def withNumber(number: com.example.Maybe[java.lang.Integer]): SimpleGreeting = {
    copy(number = number)
  }
  def withNumber(number: Int): SimpleGreeting = {
    copy(number = com.example.Maybe.just[java.lang.Integer](number))
  }
}
object SimpleGreeting {
  def apply(message: String): SimpleGreeting = new SimpleGreeting(message, com.example.Maybe.nothing[java.lang.Integer]())
  def apply(message: String, number: com.example.Maybe[java.lang.Integer]): SimpleGreeting = new SimpleGreeting(message, number)
  def apply(message: String, number: Int): SimpleGreeting = new SimpleGreeting(message, com.example.Maybe.just[java.lang.Integer](number))
}
""".stripMargin.unindent
    ))
  }

  lazy val instantiateJavaOptional: (String, String) => String =
    {
      (tpe: String, e: String) =>
        e match {
          case "null" => s"com.example.Maybe.<$tpe>nothing()"
          case e      => s"com.example.Maybe.<$tpe>just($e)"
        }
    }
  val javaLazy = "com.example.Lazy"
  val javaOptional = "com.example.Maybe"
  val outputFile = new File("output.scala")
  val scalaArray = "Vector"
  val genFileName = (_: Any) => outputFile
}
