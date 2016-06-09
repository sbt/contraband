package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  private def scalaDef2File(d: Definition) =
    d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")

  object autoImport {
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val createDatatypes = settingKey[Boolean]("Whether to generate the datatypes or not.")
    val createJsonFormats = settingKey[Boolean]("Whether to generate the JsonFormats for the datatypes or not.")
    val datatypeJavaLazy = settingKey[String]("Interface to use to provide laziness in Java.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeScalaFileNames = settingKey[Definition => File]("Mapping from `Definition` to file for Scala generator.")
    val datatypeScalaSealProtocols = settingKey[Boolean]("Seal abstract classes representing `Protocol`s in Scala.")
    val datatypeCodecPackage = settingKey[Option[String]]("Package in which to put the codec object.")
    val datatypeCodecName = settingKey[String]("Name of the codec object.")
    val datatypeCodecParents = settingKey[Seq[String]]("Parents of the serailizer object.")
    val datatypeInstantiateJavaLazy = settingKey[String => String]("Function that instantiate a lazy expression from an expression in Java.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      createDatatypes in generateDatatypes := true,
      createJsonFormats in generateDatatypes := true,
      datatypeJavaLazy in generateDatatypes := "xsbti.api.Lazy",
      datatypeSource in generateDatatypes := Defaults.configSrcSub(sourceDirectory).value / "datatype",
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      datatypeScalaSealProtocols in generateDatatypes := false,
      datatypeCodecPackage in generateDatatypes := Some("serialization"),
      datatypeCodecName in generateDatatypes := "Codec",
      datatypeCodecParents in generateDatatypes := Nil,
      datatypeInstantiateJavaLazy in generateDatatypes := { (e: String) => s"xsbti.SafeLazy($e)" },
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          (createDatatypes in generateDatatypes).value,
          (createJsonFormats in generateDatatypes).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeJavaLazy in generateDatatypes).value,
          (datatypeScalaFileNames in generateDatatypes).value,
          (datatypeScalaSealProtocols in generateDatatypes).value,
          (datatypeCodecPackage in generateDatatypes).value,
          (datatypeCodecName in generateDatatypes).value,
          (datatypeCodecParents in generateDatatypes).value,
          (datatypeInstantiateJavaLazy in generateDatatypes).value,
          streams.value)
      },
      sourceGenerators in Compile <+= generateDatatypes
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    inConfig(Compile)(baseDatatypeSettings) ++ inConfig(Test)(baseDatatypeSettings)

}

object Generate {

  private def codecFileName(genScalaFileName: Definition => File): Definition => File = d => {
    val original = genScalaFileName(d)
    val parent = original.getParentFile
    parent / "serialization" / original.getName
  }


  private def generate(createDatatypes: Boolean,
    createJsonFormats: Boolean,
    definitions: Array[File],
    target: File,
    javaLazy: String,
    scalaFileNames: Definition => File,
    scalaSealProtocols: Boolean,
    codecPackage: Option[String],
    codecName: String,
    codecParents: Seq[String],
    instantiateJavaLazy: String => String,
    log: Logger): Seq[File] = {
    val input = definitions flatMap (f => Schema.parse(IO read f).definitions)
    val fullSchema = Schema(input.toList)

    val generator = new MixedCodeGen(javaLazy, scalaFileNames, scalaSealProtocols)
    val jsonFormatsGenerator = new CodecCodeGen(codecFileName(scalaFileNames), codecPackage, codecName, codecParents, instantiateJavaLazy)

    val datatypes =
      if (createDatatypes) {
        generator.generate(fullSchema).map {
          case (file, code) =>
            val outputFile = new File(target, "/" + file.toString)
            IO.write(outputFile, code)
            log.info(s"sbt-datatype created $outputFile")

            outputFile
        }.toList
      } else {
        List.empty
      }

    val formats =
      if (createJsonFormats) {
        jsonFormatsGenerator.generate(fullSchema).map {
          case (file, code) =>
            val outputFile = new File(target, "/" + file.toString)
            IO.write(outputFile, code)
            log.info(s"sbt-datatype created $outputFile")

            outputFile
        }.toList
      } else {
        List.empty
      }

    datatypes ++ formats
  }

  def apply(base: File,
    createDatatypes: Boolean,
    createJsonFormats: Boolean,
    target: File,
    javaLazy: String,
    scalaFileNames: Definition => File,
    scalaSealProtocols: Boolean,
    codecPackage: Option[String],
    codecName: String,
    codecParents: Seq[String],
    instantiateJavaLazy: String => String,
    s: TaskStreams): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(createDatatypes, createJsonFormats, definitions, target, javaLazy, scalaFileNames, scalaSealProtocols, codecPackage, codecName, codecParents, instantiateJavaLazy, s.log)
    val f = FileFunction.cached(s.cacheDirectory / "gen-api", FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
