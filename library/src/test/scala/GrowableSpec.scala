package sbt.datatype

import org.specs2._
import SchemaExample._

class CodeGenSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    CodeGen.generate should
      generate                                   $e1
  """

  def e1 = {
    val s = ProtocolSchema.parse(growableSchema)
    val code = CodeGen.generate(s)
    code.lines.toList must containTheSameElementsAs("""package com.example

final class Greeting(val message: String,
  val name: String) {
  def this(message: String) = this(message, "foo")
  override def equals(o: Any): Boolean =
    o match {
      case x: Greeting =>
        (this.message == x.message) &&
        (this.name == x.name)
      case _ => false
    }
  override def hashCode: Int =
    {
      var hash = 1
      hash = hash * 31 + this.message.##
      hash = hash * 31 + this.name.##
      hash
    }
  private[this] def copy(message: String = this.message, name: String = this.name): Greeting =
    new Greeting(message, name)
}

object Greeting {
  def apply(message: String,
  name: String): Greeting =
    new Greeting(message, name)
}""".lines.toList)
  }
}

