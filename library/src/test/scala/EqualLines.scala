package sbt.contraband

import org.scalatest._, matchers._

trait EqualLines {
  final case class Lines(value: Vector[String]) {
    def diff(expectedName: String, obtainedName: String, right: Lines) =
      TestUtils.unifiedDiff(expectedName, obtainedName, value, right.value, 3)
  }
  private val emptyLines = Lines(Vector.empty)

  implicit class CleanedString(s: String) {
    def unindent: Lines = Lines(s.lines.map(_.trim).filterNot(_.isEmpty).toVector)
    def withoutEmptyLines: Lines = Lines(s.lines.filterNot(_.trim.isEmpty).toVector)
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
}
