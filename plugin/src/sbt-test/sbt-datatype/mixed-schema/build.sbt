import sbt.datatype._

name := "example"

datatypeFormatsForType in generateDatatypes in Compile := { tpe =>
  val substitutions = Map("java.io.File" -> "com.example.FileFormats")
  CodecCodeGen.removeTypeParameters(tpe) match {
    case TpeRef(name, _, _, _) if substitutions contains name => substitutions(name) :: Nil
    case                                                    _ => ((datatypeFormatsForType in generateDatatypes in Compile).value)(tpe) ++ Nil
  }
}

enablePlugins(DatatypePlugin)