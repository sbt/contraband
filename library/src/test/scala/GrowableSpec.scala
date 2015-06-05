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
    code must_== """package com.example

final class Greeting(message: String,
  name: String) private {
  override def equals(o: Any): Boolean =
    o match {
      case x: Greeting =>
        (this.message == o.message) &&
        (this.name == o.name)
      case _ => false
    }
  override def hashCode: Int =
    {
      var hash = 1
      hash = hash * 31 + this.message.##
      hash = hash * 31 + this.name.##
      hash
    }
}

object Greeting {
  def apply(message: String,
  name: String): Greeting =
    new Greeting(message, name)
}"""
  }
}

