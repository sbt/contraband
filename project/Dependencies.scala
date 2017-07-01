import sbt._

object Dependencies {
  val jawnVersion = "0.10.4"

  private val jsonTuples = Seq(
    // Keep json4s with this version.
    ("org.json4s", "json4s-core", "3.2.10"),
    ("org.spire-math", "jawn-parser", jawnVersion),
    ("org.spire-math", "jawn-json4s", jawnVersion)
  )

  val jsonDependencies = jsonTuples map {
    case (group, mod, version) => (group %% mod % version).exclude("org.scala-lang", "scalap")
  }

  val scalaTest         = "org.scalatest" %% "scalatest" % "3.0.0"
  val parboiled         = "org.parboiled" %% "parboiled" % "2.1.3"
  val diffutils         = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
}
