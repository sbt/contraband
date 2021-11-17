import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    Compile / generateContrabands / contrabandFormatsForType := { tpe =>
      val substitutions = Map("java.io.File" -> "com.example.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((Compile / generateContrabands / contrabandFormatsForType).value)(tpe)
    },
    Compile / generateContrabands / contrabandScalaArray := "Array",
    Compile / generateContrabands / contrabandScalaPrivateConstructor := false
  )
