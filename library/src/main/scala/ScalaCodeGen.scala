package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap

/**
 * Code generator for Scala.
 */
class ScalaCodeGen(scalaArray: String, genFile: Definition => File, sealProtocols: Boolean) extends CodeGenerator {

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


  override def generate(s: Schema): ListMap[File, String] =
    s.definitions.toList map (generate (s, _, None, Nil)) reduce (_ merge _) mapV (_.indented)

  override def generateEnum(s: Schema, e: Enumeration): ListMap[File, String] = {
    val values =
      e.values map { case (EnumerationValue(name, doc)) =>
        s"""${genDoc(doc)}
           |case object $name extends ${e.name}""".stripMargin
      } mkString EOL

    val code =
      s"""${genPackage(e)}
         |${genDoc(e.doc)}
         |sealed abstract class ${e.name} extends Serializable
         |object ${e.name} {
         |  ${e.extra mkString EOL}
         |  $values
         |}""".stripMargin

    ListMap(genFile(e) -> code)
  }

  override def generateRecord(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    val allFields = superFields ++ r.fields

    val alternativeCtors =
      genAlternativeConstructors(r.since, allFields) mkString EOL

    // If there are no fields, we still need an `apply` method with an empty parameter list.
    val applyOverloads =
      if (allFields.isEmpty) {
        s"def apply(): ${r.name} = new ${r.name}()"
      } else {
        perVersionNumber(r.since, allFields) { (provided, byDefault) =>
          val applyParameters =
            provided map genParam mkString ", "

          val ctorCallArguments =
            allFields map {
              case f if provided contains f  => f.name
              case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
            } mkString ", "

          s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)"
        } mkString EOL
      }

    val ctorParameters =
      genCtorParameters(r, allFields) mkString ","

    val superCtorArguments = superFields map (_.name) mkString ", "

    val extendsCode =
      parent map (p => s"extends ${fullyQualifiedName(p)}($superCtorArguments)") getOrElse "extends Serializable"

    val lazyMembers =
      genLazyMembers(r.fields) mkString EOL

    val code =
      s"""${genPackage(r)}
         |${genDoc(r.doc)}
         |final class ${r.name}($ctorParameters) $extendsCode {
         |  ${r.extra mkString EOL}
         |  $alternativeCtors
         |  $lazyMembers
         |  ${genEquals(r, superFields)}
         |  ${genHashCode(r, superFields)}
         |  ${genToString(r, superFields)}
         |  ${genCopy(r, superFields)}
         |  ${genWith(r, superFields)}
         |}
         |
         |object ${r.name} {
         |  $applyOverloads
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateInterface(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    val allFields = superFields ++ i.fields
    val alternativeCtors =
      genAlternativeConstructors(i.since, allFields) mkString EOL

    val ctorParameters =
      genCtorParameters(i, allFields) mkString ", "

    val superCtorArguments =
      superFields map (_.name) mkString ", "

    val extendsCode =
      parent map (p => s"extends ${fullyQualifiedName(p)}($superCtorArguments)") getOrElse "extends Serializable"

    val lazyMembers =
      genLazyMembers(i.fields) mkString EOL

    val messages =
      genMessages(i.messages) mkString EOL

    val classDef =
      if (sealProtocols) "sealed abstract class" else "abstract class"

    val code =
      s"""${genPackage(i)}
         |${genDoc(i.doc)}
         |$classDef ${i.name}($ctorParameters) $extendsCode {
         |  ${i.extra mkString EOL}
         |  $alternativeCtors
         |  $lazyMembers
         |  $messages
         |  ${genEquals(i, superFields)}
         |  ${genHashCode(i, superFields)}
         |  ${genToString(i, superFields)}
         |}""".stripMargin

    ListMap(genFile(i) -> code) :: (i.children map (generate(s, _, Some(i), superFields ++ i.fields))) reduce (_ merge _)
  }

  private def genDoc(doc: List[String]) = doc match {
    case Nil => ""
    case l :: Nil => s"/** $l */"
    case lines =>
      val doc = lines map (l => s" * $l") mkString EOL
      s"""/**
         |$doc
         | */""".stripMargin
  }

  private def genParam(f: Field): String = s"${f.name}: ${genRealTpe(f.tpe, isParam = true)}"

  private def lookupTpe(tpe: String): String = tpe match {
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

  private def genRealTpe(tpe: TpeRef, isParam: Boolean) = {
    val scalaTpe = lookupTpe(tpe.name)
    val base = tpe match {
      case x if x.repeated => s"$scalaArray[$scalaTpe]"
      case x if x.optional => s"Option[$scalaTpe]"
      case _               => scalaTpe
    }
    if (tpe.lzy && isParam) s"=> $base" else base
  }

  private def genEquals(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val comparisonCode =
      if (allFields exists (_.tpe.lzy)) {
        "super.equals(o) // We have lazy members, so use object identity to avoid circularity."
      } else if (allFields.isEmpty) {
        "true"
      } else {
        allFields map (f => s"(this.${f.name} == x.${f.name})") mkString " && "
      }

    s"""override def equals(o: Any): Boolean = o match {
       |  case x: ${cl.name} => $comparisonCode
       |  case _ => false
       |}""".stripMargin
  }

  private def genHashCode(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val computationCode =
      if (allFields exists (_.tpe.lzy)) {
        s"super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity."
      } else {
        (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${f.name}.##)" }
      }

    s"""override def hashCode: Int = {
       |  $computationCode
       |}""".stripMargin
  }

  private def genToString(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields

    if (allFields exists (_.tpe.lzy)) {
      s"""override def toString: String = {
         |  super.toString // Avoid evaluating lazy members in toString to avoid circularity.
         |}""".stripMargin
    } else if (allFields.isEmpty) {
      s"""override def toString: String = {
         |  "${cl.name}()"
         |}""".stripMargin
    } else {
      val fieldsToString =
        allFields.map(_.name).mkString(" + ", """ + ", " + """, " + ")
      s"""override def toString: String = {
         |  "${cl.name}("$fieldsToString")"
         |}""".stripMargin
    }
  }

  private def genAlternativeConstructors(since: VersionNumber, allFields: List[Field]) =
    perVersionNumber(since, allFields) {
      case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
        val ctorParameters =
          provided map genParam mkString ", "
        val thisCallArguments =
          allFields map {
            case f if provided contains f  => f.name
            case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
          } mkString ", "

        s"def this($ctorParameters) = this($thisCallArguments)"

      case _ => ""
    }

  // If a class has a lazy member, it means that the class constructor will have a call-by-name
  // parameter. Because val parameters may not be call-by-name, we prefix the parameter with `_`
  // and we will create the actual lazy val as a regular class member.
  // Non-lazy fields that belong to `cl` are made val parameters.
  private def genCtorParameters(cl: ClassLike, allFields: List[Field]): List[String] =
    allFields map {
      case f if cl.fields.contains(f) && f.tpe.lzy =>
        EOL + "_" + genParam(f)

      case f if cl.fields.contains(f) =>
        s"""$EOL${genDoc(f.doc)}
           |val ${genParam(f)}""".stripMargin

      case f =>
        EOL + genParam(f)
    }

  private def genLazyMembers(fields: List[Field]): List[String] =
    fields filter (_.tpe.lzy) map { f =>
        s"""${genDoc(f.doc)}
           |lazy val ${f.name}: ${genRealTpe(f.tpe, isParam = false)} = _${f.name}""".stripMargin
    }

  private def genMessages(messages: List[Message]): List[String] =
    messages map { case Message(name, doc, responseTpe, request) =>
      val params = request map (a => s"${a.name}: ${genRealTpe(a.tpe, isParam = true)}") mkString ", "
      val argsDoc = request flatMap {
        case Request(name, Nil, _)        => Nil
        case Request(name, doc :: Nil, _) => s"@param $name $doc" :: Nil
        case Request(name, doc, _)        =>
          val prefix = s"@param $name "
          doc.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
      }

      s"""${genDoc(doc ++ argsDoc)}
         |def $name($params): ${genRealTpe(responseTpe, isParam = false)}"""
    }

  private def fullyQualifiedName(d: Definition): String = {
    val path = d.namespace map (ns => ns + ".") getOrElse ""
    path + d.name
  }

  private def genPackage(d: Definition): String =
    d.namespace map (ns => s"package $ns") getOrElse ""

  private def genCopy(r: Record, superFields: List[Field]) = {
    val allFields = superFields ++ r.fields
    val params = allFields map (f => s"${f.name}: ${genRealTpe(f.tpe, isParam = true)} = ${f.name}") mkString ", "
    val constructorCall = allFields map (_.name) mkString ", "
    s"""private[this] def copy($params): ${r.name} = {
       |  new ${r.name}($constructorCall)
       |}""".stripMargin
  }

  private def genWith(r: Record, superFields: List[Field]) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1) ; fst.toUpperCase + rst }
    val allFields = superFields ++ r.fields

    allFields map { f =>
      s"""def with${capitalize(f.name)}(${f.name}: ${genRealTpe(f.tpe, isParam = true)}): ${r.name} = {
         |  copy(${f.name} = ${f.name})
         |}""".stripMargin
    } mkString (EOL + EOL)
  }

}
