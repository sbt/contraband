package sbt.datatype

import org.scalatest._, matchers._

abstract class GCodeGenSpec(language: String) extends FlatSpec with Matchers {
  final case class Lines(value: Vector[String]) {
    def diff(expectedName: String, obtainedName: String, right: Lines) =
      TestUtils.unifiedDiff(expectedName, obtainedName, value, right.value, 3)
  }
  private val emptyLines = Lines(Vector.empty)

  implicit class CleanedString(s: String) {
    def unindent: Lines = Lines(s.lines map (_.trim) filterNot (_.isEmpty) toVector)
    def withoutEmptyLines: Lines = Lines(s.lines filterNot (_.trim.isEmpty) toVector)
  }

  final class EqualLines(right: Lines) extends Matcher[Lines] {
    def apply(left: Lines): MatchResult = MatchResult(
      left == right,
      s"Left lines did not equal right lines:\n${right.diff("expected", "obtained", left) mkString ("\n")}",
      "Left lines equaled right lines"
    )
  }

  def equalLines(expectedLines: Lines) = new EqualLines(expectedLines)

  final class EqualMapLines(right: Map[java.io.File, Lines]) extends Matcher[Map[java.io.File, Lines]] {
    def apply(left: Map[java.io.File, Lines]): MatchResult = {
      def diff = {
        (left.keys.toSeq ++ right.keys).distinct flatMap { file =>
          val l = left.getOrElse(file, emptyLines)
          val r = right.getOrElse(file, emptyLines)
          r.diff(s"expected/$file", s"obtained/$file", l)
        } mkString "\n"
      }
      MatchResult(
        left == right,
        s"Left map lines did not equal right map lines:\n$diff",
        "Left map lines equaled right map lines"
      )
    }
  }

  def equalMapLines(expectedMapLines: Map[java.io.File, Lines]) = new EqualMapLines(expectedMapLines)

  implicit def definition2Schema(d: Definition): Schema = Schema(List(d), Some("generated"), None)

  "generate(Enumeration)" should "generate a simple enumeration" in enumerationGenerateSimple

  "generate(Interface)" should "generate a simple interface" in interfaceGenerateSimple
  it should "generate a simple interface with one child" in interfaceGenerateOneChild
  it should "generate nested interfaces" in interfaceGenerateNested
  it should "generate messages" in interfaceGenerateMessages

  "generate(Record)" should "generate a simple record" in recordGenerateSimple
  it should "grow a record from 0 to 1 field" in recordGrowZeroToOneField
  it should "grow a record from 0 to 1 to 2 fields" in recordGrowZeroToOneToTwoFields

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
  def recordGrowZeroToOneField: Unit
  def recordGrowZeroToOneToTwoFields: Unit

  def schemaGenerateComplete: Unit
  def schemaGenerateCompletePlusIndent: Unit
  def schemaGenerateTypeReferences: Unit
  def schemaGenerateTypeReferencesNoLazy: Unit
}
