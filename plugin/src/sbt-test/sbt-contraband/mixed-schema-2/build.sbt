import sbt.contraband._

lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    name := "example",
    contrabandFormatsForType in generateContrabands in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.example.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((contrabandFormatsForType in generateContrabands in Compile).value)(tpe)
    },
    contrabandScalaArray in (Compile, generateContrabands) := "Array",
    contrabandScalaPrivateConstructor in (Compile, generateContrabands) := false
  )
