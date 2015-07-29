import Dependencies._

lazy val commonSettings = Seq(
    organization in ThisBuild := "org.scala-sbt",
    version in ThisBuild := "0.1.0-SNAPSHOT",
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    scalaVersion := "2.10.5"
  )

lazy val root = (project in file(".")).
  settings(commonSettings).
  aggregate(library, plugin)

lazy val plugin = (project in file("plugin")).
  settings(commonSettings).
  settings(
    sbtPlugin := true,
    name := "datatype-plugin",
    ScriptedPlugin.scriptedSettings,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
    },
    publishLocal <<= (publishLocal) dependsOn (publishLocal in library)
  ).
  dependsOn(library)

lazy val library = project.
  settings(commonSettings).
  settings(
    name := "datatype",
    libraryDependencies ++= jsonDependencies ++ Seq(specs2 % Test)
  )
