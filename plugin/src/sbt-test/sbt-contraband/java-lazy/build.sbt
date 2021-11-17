name := "example"

Compile / generateContrabands / contrabandJavaLazy := "com.example.MyLazy"
Compile / generateContrabands / contrabandInstantiateJavaLazy := { (e: String) => s"com.example.Main.makeLazy($e)" }

enablePlugins(ContrabandPlugin, JsonCodecPlugin)
