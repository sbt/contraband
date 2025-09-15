scalaVersion := "3.7.3"

// https://github.com/sbt/contraband/issues/149
scalacOptions += "-Wunused:all"
scalacOptions += "-Werror"
name := "example"
enablePlugins(ContrabandPlugin, JsonCodecPlugin)
