package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * Code generator to produce a serializer for a given type.
 */
class SerializerCodeGen(genFile: Definition => File, serializerPackage: Option[String], serializerName: String, serializerParents: Seq[String], instantiateJavaLazy: String => String) extends CodeGenerator {

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

  override def generate(e: Enumeration): Map[File, String] = {
    val readerValues = e.values map { case EnumerationValue(v, _) => s"""case "$v" => ${e.name}.$v""" }
    val writerValues = e.values map { case EnumerationValue(v, _) => s"""case ${e.name}.$v => "$v"""" }
    val imports = sjsonImports ++ getRequiredImports(e)

    val code =
      s"""${genPackage(e)}
         |${formatImports(imports)}
         |class ${e.name}Format extends JsonFormat[${e.name}] {
         |
         |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${e.name} = {
         |    jsOpt match {
         |      case Some(js) =>
         |        unbuilder.readString(js) match {
         |          ${readerValues mkString EOL}
         |        }
         |      case None =>
         |        deserializationError("Expected JsString but found None")
         |    }
         |  }
         |
         |  override def write[J](obj: ${e.name}, builder: Builder[J]): Unit = {
         |    val str = obj match {
         |      ${writerValues mkString EOL}
         |    }
         |    builder.writeString(str)
         |  }
         |}""".stripMargin

    Map(genFile(e) -> code)
  }

  override def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    def accessField(f: Field) = {
      if (f.tpe.lzy && r.targetLang == "Java") megaType(instantiateJavaLazy(f.name))
      else f.name
    }
    val allFields = r.fields ++ superFields
    val getFields = allFields map (f => s"""val ${f.name} = unbuilder.readField[${genRealTpe(f.tpe)}]("${f.name}")""") mkString EOL
    val reconstruct = s"new ${r.name}(" + allFields.map(accessField).mkString(", ") + ")"
    val writeFields = allFields map (f => s"""builder.addField("${f.name}", obj.${f.name})""") mkString EOL
    val imports = sjsonImports ++ getRequiredImports(r) ++ importSerializer

    val code =
      s"""${genPackage(r)}
         |${formatImports(imports)}
         |class ${r.name}Format extends JsonFormat[${r.name}] {
         |
         |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${r.name} = {
         |    jsOpt match {
         |      case Some(js) =>
         |        unbuilder.beginObject(js)
         |        $getFields
         |        unbuilder.endObject()
         |        $reconstruct
         |      case None =>
         |        deserializationError("Expected JsObject but found None")
         |    }
         |  }
         |
         |  override def write[J](obj: ${r.name}, builder: Builder[J]): Unit = {
         |    builder.beginObject()
         |    $writeFields
         |    builder.endObject()
         |  }
         |}""".stripMargin

    Map(genFile(r) -> code)
  }
  override def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    val code =
      p.children match {
        case Nil =>
          val imports = sjsonImports
          s"""${genPackage(p)}
             |${formatImports(imports)}
             |class ${p.name}Format extends JsonFormat[${p.name}] {
             |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${p.name} = {
             |    deserializationError("No known implementation of ${p.name}.")
             |  }
             |  override def write[J](obj: ${p.name}, builder: Builder[J]): Unit = {
             |    serializationError("No known implementation of ${p.name}.")
             |  }
             |}""".stripMargin

        case xs =>
          val imports = sjsonImports ++ getRequiredImports(p) ++ importSerializer
          val unionFormat = s"unionFormat${xs.length}[${p.name}, ${xs map (_.name) mkString ", "}]"
          s"""${genPackage(p)}
             |${formatImports(imports)}
             |class ${p.name}Format extends JsonFormat[${p.name}] {
             |  private val format = $unionFormat
             |
             |  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ${p.name} = {
             |    format.read(jsOpt, unbuilder)
             |  }
             |
             |  override def write[J](obj: ${p.name}, builder: Builder[J]): Unit = {
             |    format.write(obj, builder)
             |  }
             |}""".stripMargin

      }

    Map(genFile(p) -> code) :: (p.children map (generate(_, Some(p), p.fields ++ superFields))) reduce (_ merge _)
  }

  override def generate(s: Schema): Map[File, String] = {
    val datatypes = s.definitions map (generate (_, None, Nil)) reduce (_ merge _) mapValues (_.indented)
    val serializer = genSerializerObject(s) mapValues (_.indented)

    datatypes merge serializer
  }


  private def genPackage(d: Definition): String =
    d.namespace map (ns => s"package $ns") getOrElse ""

  private val sjsonImports: Seq[String] =
    "_root_.sjsonnew._" ::
      "_root_.sjsonnew.BasicJsonProtocol._" ::
      Nil

  private def getRequiredImports(d: Definition): Seq[String] = {
    val currentNamespace = d.namespace getOrElse ""
    def imports(d: Definition): List[String] =
      d match {
        case Protocol(name, _, Some(namespace), _, _, _, children) if namespace != currentNamespace =>
          s"_root_.$namespace.$name" :: children.flatMap(imports)
        case p: Protocol =>
          p.children.flatMap(imports)
        case Record(name, _, Some(namespace), _, _, _) if namespace != currentNamespace =>
          s"_root_.$namespace.$name" :: Nil
        case _: Record =>
          Nil
        case Enumeration(name, _, Some(namespace), _, _, _) if namespace != currentNamespace =>
          s"_root_.$namespace.$name" :: Nil
        case _: Enumeration =>
          Nil
      }

    imports(d).distinct
  }

  private def formatImports(imports: Seq[String]) =
    imports map (i => s"import $i") mkString EOL

  private def importSerializer: Seq[String] = serializerPackage match {
    case Some(p) => s"_root_.$p.$serializerName._" :: Nil
    case None    => s"_root_.$serializerName._" :: Nil
  }

  private def genSerializerObject(s: Schema): Map[File, String] = {
    val syntheticDef = Protocol(serializerName, "Scala", None, VersionNumber("1.0.0"), None, Nil, s.definitions)
    val imports = sjsonImports ++ getRequiredImports(syntheticDef)
    val otherJsonFormats = getRequiredImports(syntheticDef) map (imp => s"import ${imp}Format") mkString EOL
    val pack = serializerPackage map (p => s"package $p") getOrElse ""
    val implicitVals = s.definitions flatMap genImplicits mkString EOL
    val parentsString = serializerParents match {
      case Nil => ""
      case x :: Nil => s"extends $x"
      case x :: xs  => s"""extends $x with ${xs mkString ","}"""
    }
    val code =
      s"""$pack
         |${formatImports(imports)}
         |$otherJsonFormats
         |object $serializerName $parentsString {
         |  $implicitVals
         |}""".stripMargin

    Map(genFile(syntheticDef) -> code)

  }

  private def genImplicits(d: Definition): List[String] = {
    def genImplicit(name: String) = s"implicit val ${name}Format: JsonFormat[$name] = new ${name}Format()"
    d match {
      case p: Protocol =>
        genImplicit(p.name) :: (p.children flatMap genImplicits)
      case r: Record =>
        genImplicit(r.name) :: Nil
      case e: Enumeration =>
        genImplicit(e.name) :: Nil
    }
  }

  private def megaType(t: String) = t.replace("<", "[").replace(">", "]")

  private def genRealTpe(tpe: TpeRef) = {
    val scalaTpe = lookupTpe(megaType(tpe.name))
    if (tpe.repeated) s"Array[$scalaTpe]" else scalaTpe
  }

  private def lookupTpe(tpe: String): String = megaType(tpe) match {
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

}
