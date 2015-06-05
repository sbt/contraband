import Dependencies._

lazy val commonSettings = Seq(
    organization in ThisBuild := "org.scala-sbt",
    version in ThisBuild := "0.1.0-SNAPSHOT",
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    scalaVersion := "2.11.6"
  )

lazy val root = (project in file(".")).
  settings(commonSettings).
  aggregate(library)

lazy val library = project.
  settings(commonSettings).
  settings(
    name := "datatype",
    libraryDependencies ++= jsonDependencies ++ Seq(specs2 % Test)
  )

