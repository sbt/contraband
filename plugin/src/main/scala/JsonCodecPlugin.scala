package sbt.contraband

import sbt.{ given, * }
import Keys.*

object JsonCodecPlugin extends AutoPlugin {
  override def requires = ContrabandPlugin
  override def trigger = noTrigger

  import ContrabandPlugin.autoImport.*
  override lazy val projectSettings =
    Vector(
      Compile / generateJsonCodecs / skipGeneration := false,
      Test / generateJsonCodecs / skipGeneration := false
    )
}
