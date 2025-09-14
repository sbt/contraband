{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("org.scala-sbt" % "sbt-contraband" % pluginVersion)
}

libraryDependencies ++= {
  if (sbtVersion.value.startsWith("1.")) {
    Seq(
      Defaults.sbtPluginExtra(
        "org.scalameta" % "sbt-scalafmt" % "2.5.5",
        sbtBinaryVersion.value,
        scalaBinaryVersion.value
      )
    )
  } else {
    // TODO
    Nil
  }
}
