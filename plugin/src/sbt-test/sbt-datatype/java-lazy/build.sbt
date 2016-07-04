name := "example"

datatypeJavaLazy in generateDatatypes in Compile := "com.example.MyLazy"
datatypeInstantiateJavaLazy in generateDatatypes in Compile := { (e: String) => s"com.example.Main.makeLazy($e)" }

enablePlugins(sbt.datatype.DatatypePlugin)