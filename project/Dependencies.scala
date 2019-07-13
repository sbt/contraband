import sbt._
import Keys._

object Dependencies {
  val jsonDependencies =
    Def.setting {
      val json4sVersion = scalaBinaryVersion.value match {
        case "2.10" | "2.11" =>
          "3.2.10"
        case "2.12" =>
          "3.4.2"
        case _ =>
          "3.6.6"
      }
      val jawn = scalaBinaryVersion.value match {
        case "2.10" | "2.11" | "2.12" =>
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

  val scalaTest         = "org.scalatest" %% "scalatest" % "3.0.8"
  val parboiled         = Def.setting{
    val v = scalaBinaryVersion.value match {
      case "2.10" =>
        "2.1.3"
      case _ =>
        "2.1.7"
    }
    "org.parboiled" %% "parboiled" % v
  }
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
}
