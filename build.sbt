import Dependencies._

lazy val commonSettings = Seq(
    organization in ThisBuild := "org.scala-sbt",
    version in ThisBuild := "0.1.0",
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    scalaVersion := "2.10.5",
    licenses += ("Apache-2.0", url("https://github.com/sbt/sbt-datatype/blob/master/LICENSE")),
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    bintrayVcsUrl := Some("git@github.com:sbt/sbt-datatype.git")
  )

lazy val root = (project in file(".")).
  aggregate(library, plugin).
  settings(
    commonSettings,
    name := "datatype root",
    publish := {},
    publishLocal := {},
    publishArtifact in Compile := false,
    publishArtifact in Test := false,
    publishArtifact := false,
    bintrayReleaseOnPublish in ThisBuild := false
  )

lazy val plugin = (project in file("plugin")).
  settings(commonSettings).
  settings(
    sbtPlugin := true,
    name := "sbt-datatype",
    description := "sbt plugin to generate growable datatypes.",
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
    sbtPlugin := true,
    name := "datatype",
    description := "Code generation library to generate growable datatypes.",
    libraryDependencies ++= jsonDependencies ++ Seq(specs2 % Test)
  )
