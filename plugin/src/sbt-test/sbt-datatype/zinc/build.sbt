import sbt.datatype._

lazy val root = (project in file(".")).
  enablePlugins(DatatypePlugin, JsonCodecPlugin).
  settings(
    name := "example",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.4.1"
    // scalacOptions += "-Xlog-implicits"
  )
