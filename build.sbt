import Dependencies._

lazy val pluginSettings = Seq(
  bintrayPackage := "sbt-contraband",
  sbtPlugin := true
)

lazy val root = (project in file(".")).
  aggregate(library, plugin).
  settings(
    inThisBuild(List(
      version := "0.3.0-SNAPSHOT",
      organization := "org.scala-sbt",
      crossScalaVersions := Seq("2.11.8", "2.10.6"),
      scalaVersion := "2.10.6",
      licenses += ("Apache-2.0", url("https://github.com/sbt/contraband/blob/master/LICENSE")),
      bintrayOrganization := Some("sbt"),
      bintrayRepository := "sbt-plugin-releases",
      bintrayVcsUrl := Some("git@github.com:sbt/contraband.git")
    )),
    name := "contraband root",
    publish := {},
    publishLocal := {},
    publishArtifact in Compile := false,
    publishArtifact in Test := false,
    publishArtifact := false,
    bintrayReleaseOnPublish in ThisBuild := false
  )

lazy val plugin = (project in file("plugin")).
  settings(
    pluginSettings,
    name := "sbt-contraband",
    description := "sbt plugin to generate growable datatypes.",
    ScriptedPlugin.scriptedSettings,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
    },
    publishLocal := (publishLocal dependsOn (publishLocal in library)).value
  ).
  dependsOn(library)

lazy val library = project.
  enablePlugins(KeywordPlugin).
  settings(
    name := "contraband",
    description := "Code generation library to generate growable datatypes.",
    libraryDependencies ++= Seq(parboiled) ++ jsonDependencies ++ Seq(scalaTest % Test, diffutils % Test)
  )
