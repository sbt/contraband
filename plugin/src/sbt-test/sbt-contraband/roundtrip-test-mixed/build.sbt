import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    contrabandFormatsForType in generateContrabands in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.foo.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((contrabandFormatsForType in generateContrabands in Compile).value)(tpe)
    },
    contrabandCodecParents in (Compile, generateContrabands) += "com.foo.MaybeFormats",
    contrabandInstantiateJavaOptional in generateContrabands in Compile := { (tpe: String, e: String) =>
      e match {
        case "null" => s"com.example.Maybe.<$tpe>nothing()"
        case e      => s"com.example.Maybe.<$tpe>just($e)"
      }
    },
    contrabandJavaOption in generateContrabands in Compile := "com.example.Maybe",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.5.1"
    // scalacOptions += "-Xlog-implicits"
  )
