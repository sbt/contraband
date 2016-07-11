package sbt.datatype

import sbt._
import Keys._

object JsonCodecPlugin extends AutoPlugin {
  override def requires = DatatypePlugin
  override def trigger = noTrigger

  import DatatypePlugin.autoImport._
  override lazy val projectSettings =
    Vector(
      skipGeneration in (Compile, generateJsonCodecs) := false,
      skipGeneration in (Test, generateJsonCodecs) := false
    )
}
