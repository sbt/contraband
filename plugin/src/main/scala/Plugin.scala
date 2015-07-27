package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  object autoImport {
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeTargetLang = settingKey[DatatypeTargetLang]("Target language for datatypes.")
    val datatypeScalaFileNames = settingKey[Definition => String]("Mapping from `Definition` to file name for Scala generator.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      datatypeSource in generateDatatypes := sourceDirectory.value / "datatype",
      datatypeTargetLang in generateDatatypes := DatatypeTargetLang.Scala,
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := ((d: Definition) => s"${d.name}.scala"),
      sourceGenerators in Compile <+= generateDatatypes,
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeTargetLang in generateDatatypes).value,
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

  def apply(base: File, target: File, mode: DatatypePlugin.autoImport.DatatypeTargetLang, scalaFileNames: Definition => String): Seq[File] = {
    val input: Array[Schema] = IO listFiles base map (f => Schema parse (IO read f))

    val generator: CodeGenerator = mode match {
      case DatatypePlugin.autoImport.DatatypeTargetLang.Java  => JavaCodeGen
      case DatatypePlugin.autoImport.DatatypeTargetLang.Scala => new ScalaCodeGen(scalaFileNames)
    }

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
