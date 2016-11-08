package sbt.datatype

import sbt._
import Keys._

object DatatypePlugin extends AutoPlugin {

  private def scalaDef2File(d: Definition) =
    d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")

  object autoImport {
    val skipGeneration = settingKey[Boolean]("skip")
    val generateDatatypes = taskKey[Seq[File]]("Generate datatypes.")
    val generateJsonCodecs = taskKey[Seq[File]]("Dummy task for generating JSON codecs.")
    val datatypeCodecsDependencies = settingKey[Seq[ModuleID]]("ModuleIDs of the default codecs.")
    val datatypeJavaLazy = settingKey[String]("Interface to use to provide laziness in Java.")
    val datatypeJavaOption = settingKey[String]("Interface to use to provide options in Java.")
    val datatypeScalaArray = settingKey[String]("Repeated type in Scala.")
    val datatypeSource = settingKey[File]("Datatype source directory.")
    val datatypeScalaFileNames = settingKey[Definition => File]("Mapping from `Definition` to file for Scala generator.")
    val datatypeScalaSealInterface = settingKey[Boolean]("Seal abstract classes representing `interface`s in Scala.")
    val datatypeCodecParents = settingKey[List[String]]("Parents to add all o of the codec object.")
    val datatypeInstantiateJavaLazy = settingKey[String => String]("Function that instantiate a lazy expression from an expression in Java.")
    val datatypeFormatsForType = settingKey[TpeRef => List[String]]("Function that maps types to the list of required codecs for them.")

    sealed trait DatatypeTargetLang
    object DatatypeTargetLang {
      case object Java extends DatatypeTargetLang
      case object Scala extends DatatypeTargetLang
    }

    lazy val baseDatatypeSettings: Seq[Def.Setting[_]] = Seq(
      skipGeneration in generateDatatypes := false,
      skipGeneration in generateJsonCodecs := true,
      datatypeCodecsDependencies in generateDatatypes := Seq("com.eed3si9n" %% "sjson-new-core" % "0.4.2"),
      datatypeJavaLazy in generateDatatypes := "xsbti.api.Lazy",
      datatypeJavaOption in generateDatatypes := "xsbti.Maybe",
      datatypeScalaArray in generateDatatypes := "Vector",
      datatypeSource in generateDatatypes := Defaults.configSrcSub(sourceDirectory).value / "datatype",
      sourceManaged in generateDatatypes := sourceManaged.value,
      datatypeScalaFileNames in generateDatatypes := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      datatypeScalaSealInterface in generateDatatypes := false,
      datatypeCodecParents in generateDatatypes := List("sjsonnew.BasicJsonProtocol"),
      datatypeInstantiateJavaLazy in generateDatatypes := { (e: String) => s"xsbti.SafeLazy($e)" },
      datatypeFormatsForType in generateDatatypes := CodecCodeGen.formatsForType,
      generateDatatypes := {
        Generate((datatypeSource in generateDatatypes).value,
          !(skipGeneration in generateDatatypes).value,
          !(skipGeneration in generateJsonCodecs).value,
          (sourceManaged in generateDatatypes).value,
          (datatypeJavaLazy in generateDatatypes).value,
          (datatypeJavaOption in generateDatatypes).value,
          (datatypeScalaArray in generateDatatypes).value,
          (datatypeScalaFileNames in generateDatatypes).value,
          (datatypeScalaSealInterface in generateDatatypes).value,
          (datatypeCodecParents in generateDatatypes).value,
          (datatypeInstantiateJavaLazy in generateDatatypes).value,
          (datatypeFormatsForType in generateDatatypes).value,
          streams.value)
      },
      sourceGenerators in Compile += generateDatatypes.taskValue
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = noTrigger

  override lazy val projectSettings =
    inConfig(Compile)(baseDatatypeSettings) ++ inConfig(Test)(baseDatatypeSettings) ++ Seq(
      libraryDependencies ++= {
        val addInCompile = !(skipGeneration in (Compile, generateJsonCodecs)).value
        val addInTest = !addInCompile && !(skipGeneration in (Test, generateJsonCodecs)).value

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
  private def generate(createDatatypes: Boolean,
    createCodecs: Boolean,
    definitions: Array[File],
    target: File,
    javaLazy: String,
    javaOption: String,
    scalaArray: String,
    scalaFileNames: Definition => File,
    scalaSealInterface: Boolean,
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    formatsForType: TpeRef => List[String],
    log: Logger): Seq[File] = {
    val input = definitions.toList map (f => Schema.parse(IO read f))
    val generator = new MixedCodeGen(javaLazy, javaOption, scalaArray, scalaFileNames, scalaSealInterface)
    val jsonFormatsGenerator = new CodecCodeGen(codecParents, instantiateJavaLazy, javaOption, scalaArray, formatsForType, input)

    val datatypes =
      if (createDatatypes) {
        input flatMap { s =>
          generator.generate(s).map {
            case (file, code) =>
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.info(s"sbt-datatype created $outputFile")
              // println(code)
              outputFile
          }.toList
        }
      } else {
        List.empty
      }

    val formats =
      if (createCodecs) {
        input flatMap { s =>
          jsonFormatsGenerator.generate(s).map {
            case (file, code) =>
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.info(s"sbt-datatype created $outputFile")

              outputFile
          }.toList
        }
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
    javaOption: String,
    scalaArray: String,
    scalaFileNames: Definition => File,
    scalaSealInterface: Boolean,
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    formatsForType: TpeRef => List[String],
    s: TaskStreams): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(createDatatypes, createCodecs, definitions, target, javaLazy, javaOption, scalaArray,
      scalaFileNames, scalaSealInterface, codecParents, instantiateJavaLazy, formatsForType, s.log)
    val f = FileFunction.cached(s.cacheDirectory / "gen-api", FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
