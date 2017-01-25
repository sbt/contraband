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
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example;
public abstract class Greeting implements java.io.Serializable {


    private String message;
    private com.example.Maybe<String> s;
    public Greeting(String _message) {
        super();
        message = _message;
        s = com.example.Maybe.<String>just("1");
    }
    public Greeting(String _message, com.example.Maybe<String> _s) {
        super();
        message = _message;
        s = _s;
    }
    public Greeting(String _message, String _s) {
        super();
        message = _message;
        s = com.example.Maybe.<String>just(_s);
    }
    public String message() {
        return this.message;
    }
    public com.example.Maybe<String> s() {
        return this.s;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Greeting)) {
            return false;
        } else {
            Greeting o = (Greeting)obj;
            return message().equals(o.message()) && s().equals(o.s());
        }
    }
    public int hashCode() {
        return 37 * (37 * (17 + message().hashCode()) + s().hashCode());
    }
    public String toString() {
        return "Greeting("  + "message: " + message() + ", " + "s: " + s() + ")";
    }
}
""".stripMargin.unindent,
        new File("output.scala") ->
          """/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example
final class SimpleGreeting private (
  message: String,
  s: com.example.Maybe[String]) extends com.example.Greeting(message, s) with Serializable {
  private def this(message: String) = this(message, com.example.Maybe.just[String]("1"))
  override def equals(o: Any): Boolean = o match {
    case x: SimpleGreeting => (this.message == x.message) && (this.s == x.s)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + message.##) + s.##)
  }
  override def toString: String = {
    "SimpleGreeting(" + message + ", " + s + ")"
  }
  protected[this] def copy(message: String = message, s: com.example.Maybe[String] = s): SimpleGreeting = {
    new SimpleGreeting(message, s)
  }
  def withMessage(message: String): SimpleGreeting = {
    copy(message = message)
  }
  def withS(s: com.example.Maybe[String]): SimpleGreeting = {
    copy(s = s)
  }
  def withS(s: String): SimpleGreeting = {
    copy(s = com.example.Maybe.just[String](s))
  }
}
object SimpleGreeting {
  def apply(message: String): SimpleGreeting = new SimpleGreeting(message, com.example.Maybe.just[String]("1"))
  def apply(message: String, s: com.example.Maybe[String]): SimpleGreeting = new SimpleGreeting(message, s)
  def apply(message: String, s: String): SimpleGreeting = new SimpleGreeting(message, com.example.Maybe.just[String](s))
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
