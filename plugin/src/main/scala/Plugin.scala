package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  private def scalaDef2File(d: Definition) =
    d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")

  object autoImport {
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val createDatatypes = settingKey[Boolean]("Whether to generate the datatypes or not.")
    val createCodecs = settingKey[Boolean]("Whether to generate the JsonFormats for the datatypes or not.")
    val datatypeCodecsDependencies = settingKey[Seq[ModuleID]]("ModuleIDs of the default codecs.")
    val datatypeJavaLazy = settingKey[String]("Interface to use to provide laziness in Java.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeScalaFileNames = settingKey[Definition => File]("Mapping from `Definition` to file for Scala generator.")
    val datatypeScalaSealProtocols = settingKey[Boolean]("Seal abstract classes representing `Protocol`s in Scala.")
    val datatypeCodecName = settingKey[String]("Name of the full codec object.")
    val datatypeCodecNamespace = settingKey[Option[String]]("Package that holds the full codec object.")
    val datatypeCodecParents = settingKey[List[String]]("Parents to add all o of the codec object.")
    val datatypeInstantiateJavaLazy = settingKey[String => String]("Function that instantiate a lazy expression from an expression in Java.")
    val datatypeFormatsForType = settingKey[TpeRef => List[String]]("Function that maps types to the list of required codecs for them.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      createDatatypes in generateDatatypes := true,
      datatypeCodecsDependencies in generateDatatypes := Seq("com.eed3si9n" %% "sjson-new-core" % "0.4.0"),
      createCodecs in generateDatatypes := true,
      datatypeJavaLazy in generateDatatypes := "xsbti.api.Lazy",
      datatypeSource in generateDatatypes := Defaults.configSrcSub(sourceDirectory).value / "datatype",
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      datatypeScalaSealProtocols in generateDatatypes := false,
      datatypeCodecName in generateDatatypes := "Codec",
      datatypeCodecNamespace in generateDatatypes := None,
      datatypeCodecParents in generateDatatypes := Nil,
      datatypeInstantiateJavaLazy in generateDatatypes := { (e: String) => s"xsbti.SafeLazy($e)" },
      datatypeFormatsForType in generateDatatypes := CodecCodeGen.formatsForType,
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          (createDatatypes in generateDatatypes).value,
          (createCodecs in generateDatatypes).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeJavaLazy in generateDatatypes).value,
          (datatypeScalaFileNames in generateDatatypes).value,
          (datatypeScalaSealProtocols in generateDatatypes).value,
          (datatypeCodecName in generateDatatypes).value,
          (datatypeCodecNamespace in generateDatatypes).value,
          (datatypeCodecParents in generateDatatypes).value,
          (datatypeInstantiateJavaLazy in generateDatatypes).value,
          (datatypeFormatsForType in generateDatatypes).value,
          streams.value)
      },
      sourceGenerators in Compile <+= generateDatatypes
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = noTrigger

  override lazy val projectSettings =
    inConfig(Compile)(baseDatatypeSettings) ++ inConfig(Test)(baseDatatypeSettings) ++ Seq(
      libraryDependencies ++= {
        val addInCompile = (createCodecs in generateDatatypes in Compile).value
        val addInTest = !addInCompile && (createCodecs in generateDatatypes in Test).value

        val inCompile =
          if (addInCompile) (datatypeCodecsDependencies in generateDatatypes in Compile).value
          else Seq.empty

        val inTest =
          if (addInTest) (datatypeCodecsDependencies in generateDatatypes in Test).value map (_ % Test)
          else Seq.empty

        inCompile ++ inTest
      }
    )
}

object Generate {

  private def codecFileName(genScalaFileName: Definition => File): Definition => File = d => {
    val original = genScalaFileName(d)
    val parent = original.getParentFile
    parent / "serialization" / original.getName
  }


  private def generate(createDatatypes: Boolean,
    createCodecs: Boolean,
    definitions: Array[File],
    target: File,
    javaLazy: String,
    scalaFileNames: Definition => File,
    scalaSealProtocols: Boolean,
    codecName: String,
    codecNamespace: Option[String],
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    formatsForType: TpeRef => List[String],
    log: Logger): Seq[File] = {
    val input = definitions flatMap (f => Schema.parse(IO read f).definitions)
    val fullSchema = Schema(input.toList)

    val generator = new MixedCodeGen(javaLazy, scalaFileNames, scalaSealProtocols)
    val jsonFormatsGenerator = new CodecCodeGen(codecFileName(scalaFileNames), codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType)

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
      if (createCodecs) {
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
    createCodecs: Boolean,
    target: File,
    javaLazy: String,
    scalaFileNames: Definition => File,
    scalaSealProtocols: Boolean,
    codecName: String,
    codecNamespace: Option[String],
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    formatsForType: TpeRef => List[String],
    s: TaskStreams): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(createDatatypes, createCodecs, definitions, target, javaLazy, scalaFileNames, scalaSealProtocols, codecName, codecNamespace, codecParents, instantiateJavaLazy, formatsForType, s.log)
    val f = FileFunction.cached(s.cacheDirectory / "gen-api", FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
