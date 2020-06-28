import sbt._
import Keys._

object Dependencies {
  val scala211 = "2.11.12"
  val scala212 = "2.12.10"
  val scala213 = "2.13.1"

  val jsonDependencies =
    Def.setting {
      val json4sVersion = scalaBinaryVersion.value match {
        case "2.11" =>
          "3.2.10"
        case "2.12" =>
          "3.4.2"
        case _ =>
          "3.6.6"
      }
      val jawn = scalaBinaryVersion.value match {
        case "2.11" | "2.12" =>
          val v = "0.10.4"
          Seq(
            "org.spire-math" %% "jawn-parser" % v,
            "org.spire-math" %% "jawn-json4s" % v
          )
        case _ =>
          val v = "0.14.2"
          Seq(
            "org.typelevel" %% "jawn-parser" % v,
            "org.typelevel" %% "jawn-json4s" % v
          )
      }
      (("org.json4s" %% "json4s-core" % json4sVersion) +: jawn).map {
        _.exclude("org.scala-lang", "scalap")
      }
    }

  val scalaTest         = "org.scalatest" %% "scalatest" % "3.2.0"
  val parboiled         = "org.parboiled" %% "parboiled" % "2.1.8"
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
  val verify            = "com.eed3si9n.verify" %% "verify" % "0.2.0"
}
