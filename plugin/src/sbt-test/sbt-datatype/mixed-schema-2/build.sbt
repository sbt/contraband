name := "example"

datatypeCodecParents in generateDatatypes in Compile := Seq("com.example.serialization.FileFormat")

libraryDependencies += "com.eed3si9n" %% "sjson-new-core" % "0.4.0"
