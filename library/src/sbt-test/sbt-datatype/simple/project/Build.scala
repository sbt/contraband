package sbt.datatype
import SchemaExample._
import sbt._
import Keys._

object BcBuild extends Build {
  lazy val root = Project("root", file(".")).settings(BcTests.settings:_*)
}

object BcTests {
  val SecondCompile = Configuration("second-compile", "The second compilation", true, List(Compile), true)
  val runBc = TaskKey[String]("runBc")
  val genSource = TaskKey[Seq[File]]("genSource")
  def settings: Seq[Setting[_]] =
    inConfig(SecondCompile)(Defaults.compileSettings) ++
    Seq(
      genSource in SecondCompile := {
        val file = sourceManaged.value / "Growable.scala"
        IO.write(file, CodeGen.generate(ProtocolSchema.parse(growableSchema)))
        Seq(file)
      },
      sourceGenerators in SecondCompile += (genSource in SecondCompile).taskValue,
      unmanagedClasspath in SecondCompile <<= fullClasspath in Compile,
      genSource in Compile := {
        val file = sourceManaged.value / "Basic.scala"
        IO.write(file, CodeGen.generate(ProtocolSchema.parse(basicSchema)))
        Seq(file)
      },
      sourceGenerators in Compile += (genSource in Compile).taskValue,
      mainClass := Some("com.example.Main"),
      runBc <<= (name, fullClasspath in SecondCompile, mainClass, runner in run, streams) map runTask,
      version := "0.1",
      scalaVersion := "2.11.6",
      name := "simpletest"
    )
  def runTask(name: String, classpath: Classpath, mainClass: Option[String], runner: ScalaRun, s: TaskStreams): String =
    runner.run(mainClass getOrElse "Main", classpath map (_.data), Seq.empty, s.log) match {
      case Some(s) if (!s.startsWith("Nonzero exit code")) => s
      case Some(x) => sys.error(x)
      case None => sys.error("BC failure")
    }
}
