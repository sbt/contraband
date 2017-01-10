import sbt._

object Dependencies {
  val jawnVersion = "0.10.4"

  private val jsonTuples = Seq(
    ("org.json4s", "json4s-core", "3.5.0"),
    ("org.spire-math", "jawn-parser", jawnVersion),
    ("org.spire-math", "jawn-json4s", jawnVersion)
  )

  val jsonDependencies = jsonTuples map {
    case (group, mod, version) => (group %% mod % version).exclude("org.scala-lang", "scalap")
  }

  val scalaCheckVersion = "1.11.5"
  val junitInterface    = "com.novocode" % "junit-interface" % "0.11"
  val scalaCheck        = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val scalaTest         = "org.scalatest" %% "scalatest" % "3.0.0"
  val parboiled         = "org.parboiled" %% "parboiled" % "2.1.3"
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
}
