package sbt.datatype

import org.scalatest._
import SchemaExample._

abstract class GCodeGenSpec(language: String) extends FlatSpec with Matchers {
  implicit class CleanedString(s: String) {
    def unindent: List[String] = s.lines.toList map (_.trim) filterNot (_.isEmpty)
    def withoutEmptyLines: List[String] = s.lines.toList filterNot (_.trim.isEmpty)
  }

  implicit def definition2Schema(d: Definition): Schema =
    Schema(List(d), Some("generated"), None)


  "generate(Enumeration)" should "generate a simple enumeration" in enumerationGenerateSimple

  "generate(Interface)" should "generate a simple interface" in interfaceGenerateSimple
  it should "generate a simple interface with one child" in interfaceGenerateOneChild
  it should "generate nested interfaces" in interfaceGenerateNested
  it should "generate messages" in interfaceGenerateMessages

  "generate(Record)" should "generate a simple record" in recordGenerateSimple
  it should "grow a record from 0 to 1 field" in recordGrowZeroToOneField

  "generate(Schema)" should "generate a complete schema" in schemaGenerateComplete
  it should "generate and indent a complete schema" in schemaGenerateCompletePlusIndent
  it should "generate correct type references" in schemaGenerateTypeReferences
  it should "generate correct type references (no lazy)" in schemaGenerateTypeReferencesNoLazy

  def enumerationGenerateSimple: Unit
  def interfaceGenerateSimple: Unit
  def interfaceGenerateOneChild: Unit
  def interfaceGenerateNested: Unit
  def interfaceGenerateMessages: Unit
  def recordGenerateSimple: Unit
  def schemaGenerateComplete: Unit
  def schemaGenerateCompletePlusIndent: Unit
  def schemaGenerateTypeReferences: Unit
  def schemaGenerateTypeReferencesNoLazy: Unit
  def recordGrowZeroToOneField: Unit
}
