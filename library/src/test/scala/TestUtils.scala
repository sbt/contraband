package sbt.datatype

import scala.collection.immutable.{ Seq => sciSeq }
import scala.collection.JavaConverters._

import difflib._

object TestUtils {
  def printUnifiedDiff(expected: sciSeq[String], obtained: sciSeq[String]) = {
    val patch = DiffUtils.diff(expected.asJava, obtained.asJava)
    val unifiedDiff = DiffUtils.generateUnifiedDiff("expected", "obtained", expected.asJava, patch, 99).asScala.toList
    unifiedDiff foreach println
  }
}
