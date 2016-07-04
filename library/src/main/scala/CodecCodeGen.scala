package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * Code generator to produce a codec for a given type.
 *
 * @param genFile             A function that maps a `Definition` to the `File` in which we should write it.
 * @param codecName           The name of the full codec object to generate.
 * @param codecNamespace      The package to which the full codec object should belong.
 * @param codecParents        The parents that appear in the self type of all codecs, and the full codec inherits from.
 * @param instantiateJavaLazy How to transform an expression to its lazy equivalent in Java.
 * @param formatsForType      Given a `TpeRef` t, returns the list of codecs needed to encode t.
 */
class CodecCodeGen(genFile: Definition => File,
  codecName: String, codecNamespace: Option[String],
  codecParents: List[String],
  instantiateJavaLazy: String => String,
  formatsForType: TpeRef => List[String]) extends CodeGenerator {

  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "  "
    override def augmentIndentAfterTrigger(s: String) =
      s.endsWith("{") ||
      (s.contains(" class ") && s.endsWith("(")) // Constructor definition
    override def reduceIndentTrigger(s: String) = s.startsWith("}")
    override def reduceIndentAfterTrigger(s: String) = s.endsWith(") {") || s.endsWith("extends Serializable {") // End of constructor definition
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Schema, e: Enumeration): Map[File, String] = {
    val readerValues = e.values map { case EnumerationValue(v, _) => s"""case "$v" => ${e.name}.$v""" }
    val writerValues = e.values map { case EnumerationValue(v, _) => s"""case ${e.name}.$v => "$v"""" }
    val selfType = makeSelfType(s, e, Nil)

    val code =
      s"""${genPackage(e)}
         |$sjsonImports
         |trait ${e.name}Format { $selfType
         |  implicit lazy val ${e.name}Format: JsonFormat[${e.name}] = new JsonFormat[${e.name}] {
         |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${e.name} = {
         |      jsOpt match {
         |        case Some(js) =>
         |          unbuilder.readString(js) match {
         |            ${readerValues mkString EOL}
         |          }
         |        case None =>
         |          deserializationError("Expected JsString but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: ${e.name}, builder: Builder[J]): Unit = {
         |      val str = obj match {
         |        ${writerValues mkString EOL}
         |      }
         |      builder.writeString(str)
         |    }
         |  }
         |}""".stripMargin

    Map(genFile(e) -> code)
  }

  override def generate(s: Schema, r: Record, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    def accessField(f: Field) = {
      if (f.tpe.lzy && r.targetLang == "Java") scalaifyType(instantiateJavaLazy(f.name))
      else f.name
    }
    val allFields = r.fields ++ superFields
    val getFields = allFields map (f => s"""val ${f.name} = unbuilder.readField[${genRealTpe(f.tpe)}]("${f.name}")""") mkString EOL
    val reconstruct = s"new ${r.name}(" + allFields.map(accessField).mkString(", ") + ")"
    val writeFields = allFields map (f => s"""builder.addField("${f.name}", obj.${f.name})""") mkString EOL
    val selfType = makeSelfType(s, r, superFields)

    val code =
      s"""${genPackage(r)}
         |$sjsonImports
         |trait ${r.name}Format { $selfType
         |  implicit lazy val ${r.name}Format: JsonFormat[${r.name}] = new JsonFormat[${r.name}] {
         |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${r.name} = {
         |      jsOpt match {
         |        case Some(js) =>
         |          unbuilder.beginObject(js)
         |          $getFields
         |          unbuilder.endObject()
         |          $reconstruct
         |        case None =>
         |          deserializationError("Expected JsObject but found None")
         |      }
         |    }
         |
         |    override def write[J](obj: ${r.name}, builder: Builder[J]): Unit = {
         |      builder.beginObject()
         |      $writeFields
         |      builder.endObject()
         |    }
         |  }
         |} """.stripMargin

    Map(genFile(r) -> code)
  }

  override def generate(s: Schema, p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    val code =
      p.children match {
        case Nil =>
          s"""${genPackage(p)}
             |$sjsonImports
             |trait ${p.name}Format {
             |  implicit lazy val ${p.name}Format: JsonFormat[${p.name}] = new JsonFormat[${p.name}] {
             |    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${p.name} = {
             |      deserializationError("No known implementation of ${p.name}.")
             |    }
             |    override def write[J](obj: ${p.name}, builder: Builder[J]): Unit = {
             |      serializationError("No known implementation of ${p.name}.")
             |    }
             |  }
             |}""".stripMargin

        case xs =>
          val unionFormat = s"unionFormat${xs.length}[${p.name}, ${xs map (c => c.namespace.getOrElse("_root_") + "." + c.name) mkString ", "}]"

          val selfType = getAllRequiredFormats(s, p, superFields).distinct match {
            case Nil => ""
            case fms => fms.mkString("self: ", " with ", " =>")
          }
          s"""${genPackage(p)}
             |$sjsonImports
             |trait ${p.name}Format { $selfType
             |  implicit lazy val ${p.name}Format: JsonFormat[${p.name}] = $unionFormat
             |}""".stripMargin

      }

    Map(genFile(p) -> code) :: (p.children map (generate(s, _, Some(p), p.fields ++ superFields))) reduce (_ merge _)
  }

  override def generate(s: Schema): Map[File, String] = {
    val codecs = s.definitions map (generate (s, _, None, Nil)) reduce (_ merge _) mapValues (_.indented)
    val fullCodec = generateFullCodec(s)

    codecs merge fullCodec
  }

  /**
   * Returns the list of fully qualified codec names that we (non-transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   */
  private def getRequiredFormats(s: Schema, d: Definition, superFields: List[Field]): List[String] = {
    val typeFormats =
      d match {
        case Protocol(name, _, namespace, _, _, fields, _, _) =>
          val allFields = fields ++ superFields
          allFields flatMap (f => formatsForType(f.tpe))

        case Record(_, _, _, _, _, fields) =>
          val allFields = fields ++ superFields
          allFields flatMap (f => formatsForType(f.tpe))

        case _: Enumeration =>
          "sjsonnew.BasicJsonProtocol" :: Nil
      }

    val unionFormat = if (requiresUnionFormats(s, d, superFields)) "sjsonnew.BasicJsonProtocol" :: Nil else Nil

    typeFormats ++ unionFormat ++ codecParents
  }

  private def getAllRequiredFormats(s: Schema): List[String] = {
    val topFormats =
      s.definitions flatMap { d =>
        val tpe = TpeRef((d.namespace.map(_ + ".").getOrElse("")) + d.name, false, false)
        formatsForType(tpe)
      }

    val childrenFormats = s.definitions flatMap (getAllRequiredFormats(s, _, Nil))

    topFormats ++ childrenFormats
  }

  /**
   * Returns the list of fully qualified codec names that we (transitively) need to generate a codec for `d`,
   * knowing that it inherits fields `superFields` in the context of schema `s`.
   *
   * If `d` is a `Protocol`, we recurse to get the codecs required by its children.
   */
  private def getAllRequiredFormats(s: Schema, d: Definition, superFields: List[Field]): List[String] = d match {
    case p: Protocol =>
      getRequiredFormats(s, p, superFields) ++
        p.children.flatMap(c => getAllRequiredFormats(s, c, p.fields ++ superFields)) ++
        p.children.map(c => s"""${c.namespace getOrElse "_root_"}.${c.name}Format""")
    case r: Record =>
      getRequiredFormats(s, r, superFields)
    case e: Enumeration =>
      getRequiredFormats(s, e, Nil)
  }

  /**
   * Returns the self type declaration of the codec for `d`, knowing that it inherits fields `superFields`
   * in the context of schema `s`.
   */
  private def makeSelfType(s: Schema, d: Definition, superFields: List[Field]): String =
    getRequiredFormats(s, d, superFields).distinct match {
      case Nil => ""
      case fms => fms.mkString("self: ", " with ", " =>")
    }

  private def genPackage(d: Definition): String =
    d.namespace map (ns => s"package $ns") getOrElse ""

  private val sjsonImports: String = "import _root_.sjsonnew.{ deserializationError, serializationError, Builder, JsonFormat, Unbuilder }"

  private def scalaifyType(t: String) = t.replace("<", "[").replace(">", "]")

  private def genRealTpe(tpe: TpeRef) = {
    val scalaTpe = lookupTpe(scalaifyType(tpe.name))
    if (tpe.repeated) s"Array[$scalaTpe]" else scalaTpe
  }

  private def lookupTpe(tpe: String): String = scalaifyType(tpe) match {
    case "boolean" => "Boolean"
    case "byte"    => "Byte"
    case "char"    => "Char"
    case "float"   => "Float"
    case "int"     => "Int"
    case "long"    => "Long"
    case "short"   => "Short"
    case "double"  => "Double"
    case other     => other
  }

  private def generateFullCodec(s: Schema): Map[File, String] = {
    val allFormats = getAllRequiredFormats(s).distinct
    val selfType = allFormats match {
      case Nil  => ""
      case fmts => fmts.mkString("self: ", " with ", " =>")
    }
    val parents = codecName :: allFormats mkString ("extends ", " with ", "")
    val code =
      s"""${codecNamespace map (p => s"package $p") getOrElse ""}
         |trait $codecName { $selfType }
         |object $codecName $parents""".stripMargin

    val syntheticDefinition = Protocol(codecName, "Scala", codecNamespace, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil)

    Map(genFile(syntheticDefinition) -> code)
  }

  private def allChildrenOf(d: Definition): List[Definition] = d match {
    case p: Protocol => p :: p.children.flatMap(allChildrenOf)
    case r: Record => r :: Nil
    case e: Enumeration => e :: Nil
  }

  private def definitionsMap(s: Schema): Map[String, Definition] = {
    val allDefinitions = s.definitions flatMap allChildrenOf
    allDefinitions.map(d => d.namespace.map(_ + ".").getOrElse("") + d.name -> d).toMap
  }

  private def requiresUnionFormats(s: Schema, d: Definition, superFields: List[Field]): Boolean = d match {
    case _: Protocol => true
    case r: Record =>
      val defsMap = definitionsMap(s)
      val allFields = r.fields ++ superFields
      allFields exists { f => defsMap get f.tpe.name exists { case _: Protocol => true ; case _ => false } }
    case _: Enumeration => false
  }


}

object CodecCodeGen {
  /** Removes all type parameters from `tpe` */
  def removeTypeParameters(tpe: TpeRef): TpeRef = tpe.copy(name = removeTypeParameters(tpe.name))

  /** Removes all type parameters from `tpe` */
  def removeTypeParameters(tpe: String): String = tpe.replaceAll("<.+>", "").replaceAll("\\[.+\\]", "")

  /**
   * A function that, given a `TpeRef`, returns the list of `JsonFormat`s that are required to encode
   * and decode the given `TpeRef`.
   *
   * A non-primitive types `com.example.Tpe` (except java.io.File) is mapped to `com.example.TpeFormat`.
   */
  val formatsForType: TpeRef => List[String] =
    extensibleFormatsForType {
      removeTypeParameters(_) match {
        case TpeRef(name, _, _) if name contains "." => s"${name}Format" :: Nil
        case TpeRef(name, _, _)                      => s"_root_.${name}Format" :: Nil
      }
    }

  /**
   * Creates a mapping that maps all primitive types to provided codecs and uses `forOthers`
   * to determine mapping for non-primitive types.
   */
  def extensibleFormatsForType(forOthers: TpeRef => List[String]): TpeRef => List[String] = { tpe =>
    val basicJsonProtcol = "sjsonnew.BasicJsonProtocol"
    removeTypeParameters(tpe).name match {
      case "boolean" | "byte" | "char" | "float" | "int" | "long" | "short" | "double" | "String" => basicJsonProtcol :: Nil
      case _ => forOthers(tpe) ++ (basicJsonProtcol :: Nil)
    }
  }
}