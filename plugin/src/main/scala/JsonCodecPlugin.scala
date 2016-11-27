package sbt.contraband

import sbt._
import Keys._

object JsonCodecPlugin extends AutoPlugin {
  override def requires = ContrabandPlugin
  override def trigger = noTrigger

  import ContrabandPlugin.autoImport._
  override lazy val projectSettings =
    Vector(
      skipGeneration in (Compile, generateJsonCodecs) := false,
      skipGeneration in (Test, generateJsonCodecs) := false
    )
}
