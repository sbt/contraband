import Dependencies._

lazy val commonSettings = Seq(
    organization in ThisBuild := "org.scala-sbt",
    crossScalaVersions := Seq("2.11.8", "2.10.6"),
    scalaVersion := "2.10.6",
    licenses += ("Apache-2.0", url("https://github.com/sbt/sbt-datatype/blob/master/LICENSE")),
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    bintrayVcsUrl := Some("git@github.com:sbt/sbt-datatype.git")
  )

lazy val pluginSettings = commonSettings ++ Seq(
  bintrayPackage := "sbt-datatype",
  version := "0.2.9-SNAPSHOT",
  sbtPlugin := true
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
  settings(
    pluginSettings,
    name := "sbt-datatype",
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
    pluginSettings,
    name := "datatype",
    description := "Code generation library to generate growable datatypes.",
    libraryDependencies ++= jsonDependencies ++ Seq(scalaTest % Test),
    libraryDependencies += "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0" % Test
  )
