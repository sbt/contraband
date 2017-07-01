import sbt._
import Keys._

object Dependencies {
  val jawnVersion = "0.10.4"

  val jsonDependencies =
    Def.setting {
      val jsonTuples = Seq(
        // we are going to use older json4s intentionally to match bintry use etc.
        ("org.json4s", "json4s-core", 
        scalaBinaryVersion.value match {
          case "2.10" | "2.11" => "3.2.10"
          case _               => "3.4.2"
        }),
        ("org.spire-math", "jawn-parser", jawnVersion),
        ("org.spire-math", "jawn-json4s", jawnVersion)
      )
      jsonTuples map {
        case (group, mod, version) => (group %% mod % version).exclude("org.scala-lang", "scalap")
      }
    }

  val scalaTest         = "org.scalatest" %% "scalatest" % "3.0.0"
  val parboiled         = "org.parboiled" %% "parboiled" % "2.1.3"
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
}
