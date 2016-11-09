import sbt.datatype._

lazy val root = (project in file(".")).
  enablePlugins(DatatypePlugin, JsonCodecPlugin).
  settings(
    name := "example",
    datatypeFormatsForType in generateDatatypes in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.foo.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((datatypeFormatsForType in generateDatatypes in Compile).value)(tpe)
    },
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.4.1"
    // scalacOptions += "-Xlog-implicits"
  )
