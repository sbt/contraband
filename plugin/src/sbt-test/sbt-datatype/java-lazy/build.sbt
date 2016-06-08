name := "example"

datatypeJavaLazy in generateDatatypes in Compile := "com.example.MyLazy"
datatypeInstantiateJavaLazy in generateDatatypes in Compile := { (e: String) => s"com.example.Main.makeLazy($e)" }

libraryDependencies += "com.eed3si9n" %% "sjson-new-core" % "0.4.0"
