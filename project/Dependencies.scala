import sbt._

object Dependencies {
  val picklingVersion = "0.10.1"
  val pickling = "org.scala-lang.modules" %% "scala-pickling" % picklingVersion

  private val jsonTuples = Seq(
    ("org.json4s", "json4s-core", "3.2.10"),
    ("org.spire-math", "jawn-parser", "0.6.0"),
    ("org.spire-math", "json4s-support", "0.6.0")
  )

  val jsonDependencies = jsonTuples map {
    case (group, mod, version) => (group %% mod % version).exclude("org.scala-lang", "scalap")
  }

  val scalaCheckVersion = "1.11.5"
  val junitInterface       = "com.novocode" % "junit-interface" % "0.11"
  val scalaCheck           = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val specs2               = "org.specs2" %% "specs2" % "2.3.11"
}

