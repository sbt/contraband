package sbt.contraband

import sbt._
import Keys._
import ast._
import parser.{ JsonParser, SchemaParser }
import scala.util.Success

object ContrabandPlugin extends AutoPlugin {

  private def scalaDef2File(x: Any) =
    x match {
      case d: TypeDefinition => d.namespace map (ns => new File(ns.replace(".", "/"))) map (new File(_, d.name + ".scala")) getOrElse new File(d.name + ".scala")
    }

  object autoImport {
    val skipGeneration = settingKey[Boolean]("skip")
    val generateContrabands = taskKey[Seq[File]]("Generate contraband classes.")
    val generateJsonCodecs = taskKey[Seq[File]]("Dummy task for generating JSON codecs.")
    val contrabandCodecsDependencies = settingKey[Seq[ModuleID]]("ModuleIDs of the default codecs.")
    val contrabandJavaLazy = settingKey[String]("Interface to use to provide laziness in Java.")
    val contrabandJavaOption = settingKey[String]("Interface to use to provide options in Java.")
    val contrabandScalaArray = settingKey[String]("Repeated type in Scala.")
    val contrabandSource = settingKey[File]("Contraband source directory.")
    val contrabandScalaFileNames = settingKey[Any => File]("Mapping from `Definition` to file for Scala generator.")
    val contrabandScalaSealInterface = settingKey[Boolean]("Seal abstract classes representing `interface`s in Scala.")
    val contrabandScalaPrivateConstructor = settingKey[Boolean]("Hide the constructors in Scala.")
    val contrabandWrapOption = settingKey[Boolean]("Provide constructors that automatically wraps the options.")
    val contrabandCodecParents = settingKey[List[String]]("Parents to add all o of the codec object.")
    val contrabandInstantiateJavaLazy = settingKey[String => String]("Function that instantiate a lazy expression from an expression in Java.")
    val contrabandInstantiateJavaOptional = settingKey[(String, String) => String]("Function that instantiate a optional expression from an expression in Java.")
    val contrabandFormatsForType = settingKey[Type => List[String]]("Function that maps types to the list of required codecs for them.")

    sealed trait ContrabandTargetLang
    object ContrabandTargetLang {
      case object Java extends ContrabandTargetLang
      case object Scala extends ContrabandTargetLang
    }

    lazy val baseContrabandSettings: Seq[Def.Setting[_]] = Seq(
      skipGeneration in generateContrabands := false,
      skipGeneration in generateJsonCodecs := true,
      contrabandCodecsDependencies in generateContrabands := Seq("com.eed3si9n" %% "sjson-new-core" % "0.6.0"),
      contrabandJavaLazy in generateContrabands := "xsbti.api.Lazy",
      contrabandJavaOption in generateContrabands := "xsbti.Maybe",
      contrabandScalaArray in generateContrabands := "Vector",
      contrabandSource in generateContrabands := Defaults.configSrcSub(sourceDirectory).value / "contraband",
      sourceManaged in generateContrabands := sourceManaged.value,
      contrabandScalaFileNames in generateContrabands := scalaDef2File,
      // We cannot enable this by default, because the default function for naming Scala files that we provide
      // will create a separate file for every `Definition`.
      contrabandScalaSealInterface in generateContrabands := false,
      contrabandScalaPrivateConstructor in generateContrabands := true,
      contrabandWrapOption in generateContrabands := true,
      contrabandCodecParents in generateContrabands := List("sjsonnew.BasicJsonProtocol"),
      contrabandInstantiateJavaLazy in generateContrabands := { (e: String) => s"xsbti.SafeLazy($e)" },
      contrabandInstantiateJavaOptional in generateContrabands := { (tpe: String, e: String) =>
        if (e == "null") s"xsbti.Maybe.<$tpe>nothing()"
        else s"xsbti.Maybe.<$tpe>just($e)"
      },
      contrabandFormatsForType in generateContrabands := CodecCodeGen.formatsForType,
      generateContrabands := {
        Generate((contrabandSource in generateContrabands).value,
          !(skipGeneration in generateContrabands).value,
          !(skipGeneration in generateJsonCodecs).value,
          (sourceManaged in generateContrabands).value,
          (contrabandJavaLazy in generateContrabands).value,
          (contrabandJavaOption in generateContrabands).value,
          (contrabandScalaArray in generateContrabands).value,
          (contrabandScalaFileNames in generateContrabands).value,
          (contrabandScalaSealInterface in generateContrabands).value,
          (contrabandScalaPrivateConstructor in generateContrabands).value,
          (contrabandWrapOption in generateContrabands).value,
          (contrabandCodecParents in generateContrabands).value,
          (contrabandInstantiateJavaLazy in generateContrabands).value,
          (contrabandInstantiateJavaOptional in generateContrabands).value,
          (contrabandFormatsForType in generateContrabands).value,
          streams.value)
      },
      sourceGenerators in Compile += generateContrabands.taskValue
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = noTrigger

  override lazy val projectSettings =
    inConfig(Compile)(baseContrabandSettings) ++ inConfig(Test)(baseContrabandSettings) ++ Seq(
      libraryDependencies ++= {
        val addInCompile = !(skipGeneration in (Compile, generateJsonCodecs)).value
        val addInTest = !addInCompile && !(skipGeneration in (Test, generateJsonCodecs)).value

        val inCompile =
          if (addInCompile) (contrabandCodecsDependencies in generateContrabands in Compile).value
          else Seq.empty

        val inTest =
          if (addInTest) (contrabandCodecsDependencies in generateContrabands in Test).value map (_ % Test)
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
    scalaFileNames: Any => File,
    scalaSealInterface: Boolean,
    scalaPrivateConstructor: Boolean,
    wrapOption: Boolean,
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    instantiateJavaOptional: (String, String) => String,
    formatsForType: Type => List[String],
    log: Logger): Seq[File] = {
    val jsonFiles = definitions.toList collect {
      case f: File if f.getName endsWith ".json" => f
    }
    val contraFiles = definitions.toList collect {
      case f: File if (f.getName endsWith ".contra") || (f.getName endsWith ".gql") => f
    }
    val input =
      (jsonFiles map { f => JsonParser.Document.parse(IO read f) }) ++
      (contraFiles map { f =>
        val ast = SchemaParser.parse(IO read f).get
        ast
      })
    val generator = new MixedCodeGen(javaLazy, javaOption, instantiateJavaOptional,
      scalaArray, scalaFileNames, scalaSealInterface, scalaPrivateConstructor, wrapOption)
    val jsonFormatsGenerator = new CodecCodeGen(codecParents, instantiateJavaLazy,
      javaOption, scalaArray, formatsForType, input)

    val datatypes =
      if (createDatatypes) {
        input flatMap { s =>
          generator.generate(s).map {
            case (file, code) =>
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.info(s"sbt-contraband created $outputFile")
              // println(code)
              // println("---------")
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
              // println(code)
              // println("---------")
              val outputFile = new File(target, "/" + file.toString)
              IO.write(outputFile, code)
              log.info(s"sbt-contraband created $outputFile")

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
    scalaFileNames: Any => File,
    scalaSealInterface: Boolean,
    scalaPrivateConstructor: Boolean,
    wrapOption: Boolean,
    codecParents: List[String],
    instantiateJavaLazy: String => String,
    instantiateJavaOptional: (String, String) => String,
    formatsForType: Type => List[String],
    s: TaskStreams): Seq[File] = {
    val definitions = IO listFiles base
    def gen() = generate(createDatatypes, createCodecs, definitions, target, javaLazy, javaOption, scalaArray,
      scalaFileNames, scalaSealInterface, scalaPrivateConstructor, wrapOption,
      codecParents, instantiateJavaLazy, instantiateJavaOptional, formatsForType, s.log)
    val f = FileFunction.cached(s.cacheDirectory / "gen-api", FilesInfo.hash) { _ => gen().toSet } // TODO: check if output directory changed
    f(definitions.toSet).toSeq
  }
}
