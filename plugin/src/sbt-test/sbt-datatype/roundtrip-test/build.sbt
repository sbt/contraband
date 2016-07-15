import sbt.datatype._

lazy val root = (project in file(".")).
  enablePlugins(DatatypePlugin, JsonCodecPlugin).
  settings(
    name := "example",
    datatypeFormatsForType in generateDatatypes in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.foo.FileFormats")
      CodecCodeGen.removeTypeParameters(tpe) match {
        case TpeRef(name, _, _, _) if substitutions contains name => substitutions(name) :: Nil
        case                                                    _ => ((datatypeFormatsForType in generateDatatypes in Compile).value)(tpe)
      }
    },
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.4.1"
    // scalacOptions += "-Xlog-implicits"
  )
