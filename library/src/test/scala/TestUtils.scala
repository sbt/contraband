package sbt.contraband

import scala.collection.immutable.{ Seq => sciSeq }
import scala.collection.JavaConverters._

import difflib._

object TestUtils {
  def unifiedDiff(
      expectedName: String,
      obtainedName: String,
      expected: sciSeq[String],
      obtained: sciSeq[String],
      contextSize: Int
  ): Vector[String] = {
    val patch = DiffUtils.diff(expected.asJava, obtained.asJava)
    DiffUtils.generateUnifiedDiff(expectedName, obtainedName, expected.asJava, patch, contextSize).asScala.toVector
  }

  def printUnifiedDiff(expected: sciSeq[String], obtained: sciSeq[String]) =
    unifiedDiff("expected", "obtained", expected, obtained, 99) foreach println
}
