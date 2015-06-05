package sbt.datatype
import SchemaExample._
import sbt._
import Keys._

trait BcResult
case class RuntimeFailure(proj: String) extends BcResult
case class Success(proj: String) extends BcResult

object BcBuild extends Build {
//  def defaultSettings: Seq[Setting[_]] = BcTests.settings
  val root = Project("root", file(".")).configs(BcTests.SecondCompile).settings(BcTests.settings:_*)
}

object BcTests {
  val SecondCompile = Configuration("second-compile", "The second compilation", true, List(Compile), true)
  val runBc = TaskKey[BcResult]("bc-run")
  val genSource = TaskKey[Seq[File]]("genSource")
  def settings: Seq[Setting[_]] =
    inConfig(SecondCompile)(Defaults.compileSettings) ++
    Seq(
//      scalaSource in SecondCompile <<= sourceDirectory in Compile apply (_ / "scala1"),
      genSource in SecondCompile := {
        val file = sourceManaged.value / "Basic.scala"
        IO.write(file, CodeGen.generate(ProtocolSchema.parse(basicSchema)))
        Seq(file)
      },
      sourceGenerators in SecondCompile += (genSource in SecondCompile).taskValue,
      unmanagedClasspath in SecondCompile <<= fullClasspath in Compile,
      mainClass := Some("Main"),
      runBc <<= (name, fullClasspath in SecondCompile, mainClass, runner in run, streams) map runTask,
      version := "0.1",
      scalaVersion := "2.11.6",
      name := "simpletest"
    )
  def runTask(name: String, classpath: Classpath, mainClass: Option[String], runner: ScalaRun, s: TaskStreams): BcResult =
    runner.run(mainClass getOrElse "Main", classpath map (_.data), Seq.empty, s.log) match {
       case Some(msg) => Success(name)
       case None      => RuntimeFailure(name)
    }
}
