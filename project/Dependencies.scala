import sbt._
import Keys._

object Dependencies {
  val scala212 = "2.12.15"
  val scala213 = "2.13.8"
  val scala3 = "3.3.0"

  val sjsonNewScalaJson = "com.eed3si9n" %% "sjson-new-scalajson" % "0.10.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16"
  val parboiled = "org.parboiled" %% "parboiled" % "2.5.0"
  val diffutils = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
  val verify = "com.eed3si9n.verify" %% "verify" % "1.0.0"
}
