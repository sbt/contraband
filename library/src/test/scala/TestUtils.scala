package sbt.contraband

import scala.collection.immutable.Seq as sciSeq
import scala.collection.JavaConverters.*

import difflib.*

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
