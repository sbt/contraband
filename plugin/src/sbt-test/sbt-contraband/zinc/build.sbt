import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % contrabandSjsonNewVersion.value,
    // scalacOptions += "-Xlog-implicits"
    Compile / generateContrabands / contrabandFormatsForType := { tpe =>
      val substitutions = Map("Integer" -> "com.foo.IntegerFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((Compile / generateContrabands / contrabandFormatsForType).value)(tpe)
    }
  )
