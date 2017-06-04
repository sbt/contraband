import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    scalaVersion := "2.10.6",
    contrabandFormatsForType in generateContrabands in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.foo.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((contrabandFormatsForType in generateContrabands in Compile).value)(tpe)
    },
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % contrabandSjsonNewVersion.value
    // scalacOptions += "-Xlog-implicits"
  )
