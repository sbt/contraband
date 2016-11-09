import sbt.datatype._

lazy val root = (project in file(".")).
  enablePlugins(DatatypePlugin, JsonCodecPlugin).
  settings(
    name := "example",
    datatypeFormatsForType in generateDatatypes in Compile := { tpe =>
      val substitutions = Map("java.io.File" -> "com.example.FileFormats")
      val name = tpe.removeTypeParameters.name
      if (substitutions contains name) substitutions(name) :: Nil
      else ((datatypeFormatsForType in generateDatatypes in Compile).value)(tpe)
    },
    datatypeScalaArray in (Compile, generateDatatypes) := "Array"
  )
