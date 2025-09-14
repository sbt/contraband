scalaVersion := "3.3.6"

enablePlugins(ContrabandPlugin, JsonCodecPlugin)

InputKey[Unit]("check") := {
  val expectFile = Def.spaceDelimited().parsed.head
  val actual = IO.read((Compile / sourceManaged).value / "com/example/Person.scala")
  val expect = IO.read(file(expectFile))
  if (sbtVersion.value.startsWith("1.")) {
    assert(actual == expect)
  } else {
    // TODO enable test if sbt-scalafmt for sbt 2 released
  }
}
