package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  private def scalaDef2File(d: Definition) =
    d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")

  object autoImport {
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeScalaFileNames = settingKey[Definition => File]("Mapping from `Definition` to file for Scala generator.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      datatypeSource in generateDatatypes := sourceDirectory.value / "datatype",
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := scalaDef2File,
      sourceGenerators in Compile <+= generateDatatypes,
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeScalaFileNames in generateDatatypes).value)
      }
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    baseDatatypeSettings

}

object Generate {

  def apply(base: File, target: File, scalaFileNames: Definition => File): Seq[File] = {
    val input: Array[Schema] = IO listFiles base map (f => Schema parse (IO read f))

    val generator: CodeGenerator = new MixedCodeGen(scalaFileNames)

    input flatMap generator.generate map {
      case (file, code) =>
        val outputFile = new File(target, "/" + file.toString)
        IO.write(outputFile, code)
        println("Created " + outputFile)

        IO.write(new File("/Users/martin/Desktop/java/" + file.toString), code)

        outputFile
    }
  }
}
