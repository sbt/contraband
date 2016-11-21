name := "example"

contrabandJavaLazy in generateContrabands in Compile := "com.example.MyLazy"
contrabandInstantiateJavaLazy in generateContrabands in Compile := { (e: String) => s"com.example.Main.makeLazy($e)" }

enablePlugins(ContrabandPlugin, JsonCodecPlugin)
