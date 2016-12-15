import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.6.0"
    // scalacOptions += "-Xlog-implicits"
  )
