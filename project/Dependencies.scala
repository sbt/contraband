import sbt._
import Keys._

object Dependencies {
  val scala212 = "2.12.11"
  val scala213 = "2.13.3"

  val sjsonNewScalaJson = "com.eed3si9n" %% "sjson-new-scalajson" % "0.9.0"
  val scalaTest         = "org.scalatest" %% "scalatest" % "3.2.0"
  val parboiled         = "org.parboiled" %% "parboiled" % "2.1.8"
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
  val verify            = "com.eed3si9n.verify" %% "verify" % "0.2.0"
}
