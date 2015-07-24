package sbt.datatype

import org.specs2._
import org.specs2.matcher.MatchResult
import SchemaExample._

abstract class GCodeGenSpec(language: String) extends Specification {
  implicit class CleanedString(s: String) {
    def unindent: List[String] = s.lines.toList map (_.trim) filterNot (_.isEmpty)
    def withoutEmptyLines: List[String] = s.lines.toList filterNot (_.trim.isEmpty)
  }

  def is = s2"""
    This is a specification for the generation of $language code.

    generate(Enumeration) should
      generate a simple enumeration              $enumerationGenerateSimple

    generate(Protocol) should
      generate a simple protocol                 $protocolGenerateSimple
      generate a simple protocol with one child  $protocolGenerateOneChild
      generate nested protocols                  $protocolGenerateNested

    generate(Record) should
      generate a simple record                   $recordGenerateSimple

    generate(Schema) should
      generate a complete schema                 $schemaGenerateComplete
      generate and indent a complete schema      $schemaGenerateCompletePlusIndent
  """

  def enumerationGenerateSimple: MatchResult[_]
  def protocolGenerateSimple: MatchResult[_]
  def protocolGenerateOneChild: MatchResult[_]
  def protocolGenerateNested: MatchResult[_]
  def recordGenerateSimple: MatchResult[_]
  def schemaGenerateComplete: MatchResult[_]
  def schemaGenerateCompletePlusIndent: MatchResult[_]

}


class GrowableSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    CodeGen.generate should
      generate                                   $e1
  """

  def e1 = {
    val s = ProtocolSchema.parse(basicSchema)
    val code = CodeGen.generate(s)
    code must_== """package com.example

final class Greeting(val message: String) {
  
  override def equals(o: Any): Boolean =
    o match {
      case x: Greeting =>
        (this.message == x.message)
      case _ => false
    }
  override def hashCode: Int =
    {
      var hash = 1
      hash = hash * 31 + this.message.##
      hash
    }
  private[this] def copy(message: String = this.message): Greeting =
    new Greeting(message)
}

object Greeting {
  def apply(message: String): Greeting =
    new Greeting(message)
}"""
  }
}

