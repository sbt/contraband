import Dependencies._

ThisBuild / version := "0.5.0-SNAPSHOT"
ThisBuild / organization := "org.scala-sbt"
ThisBuild / crossScalaVersions := Seq(scala213, scala212)
ThisBuild / scalaVersion := "2.12.12"
ThisBuild / organizationName := "sbt"
ThisBuild / organizationHomepage := Some(url("http://scala-sbt.org/"))
ThisBuild / homepage := Some(url("http://scala-sbt.org/contraband"))
ThisBuild / licenses += ("Apache-2.0", url("https://github.com/sbt/contraband/blob/master/LICENSE"))
ThisBuild / bintrayVcsUrl := Some("git@github.com:sbt/contraband.git")
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/sbt/contraband"), "git@github.com:sbt/contraband.git"))
ThisBuild / developers := List(
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n")),
  Developer("dwijnand", "Dale Wijnand", "@dwijnand", url("https://github.com/dwijnand")),
  Developer("Duhemm", "Martin Duhem", "@Duhemm", url("https://github.com/Duhemm"))
)
ThisBuild / description := "Contraband is a description language for your datatypes and APIs, currently targeting Java and Scala."

lazy val root = (project in file("."))
  .enablePlugins(TravisSitePlugin)
  .aggregate(library, plugin)
  .settings(
    name := "contraband root",
    siteGithubRepo := "sbt/contraband",
    siteEmail := { "eed3si9n" + "@" + "gmail.com" },
    publish / skip := true,
  )

lazy val library = (project in file("library"))
  .enablePlugins(KeywordPlugin, SonatypePublish)
  .disablePlugins(BintrayPlugin)
  .settings(
    name := "contraband",
    unmanagedSourceDirectories in Compile += {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          baseDirectory.value / "src/main/scala-2.13-"
        case _ =>
          baseDirectory.value / "src/main/scala-2.13+"
      }
    },
    testFrameworks += new TestFramework("verify.runner.Framework"),
    libraryDependencies ++= Seq(parboiled, sjsonNewScalaJson) ++ Seq(verify % Test, scalaTest % Test, diffutils % Test)
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BintrayPublish, SbtPlugin)
  .dependsOn(library)
  .settings(
    name := "sbt-contraband",
    bintrayPackage := "sbt-contraband",
    description := "sbt plugin to generate growable datatypes.",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    crossScalaVersions := Seq(scala212),
    publishLocal := (publishLocal dependsOn (publishLocal in library)).value
  )
