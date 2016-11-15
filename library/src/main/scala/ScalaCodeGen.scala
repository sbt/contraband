package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap
import CodeGen.bq

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
    override def reduceIndentAfterTrigger(s: String) = s.endsWith(") {") || s.endsWith(" Serializable {") // End of constructor definition
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }


  override def generate(s: Schema): ListMap[File, String] =
    s.definitions map (generate (s, _, None, Nil)) reduce (_ merge _) mapV (_.indented)

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

    val ctorParameters = genCtorParameters(r, allFields) mkString ","
    val superCtorArguments = superFields map (_.name) mkString ", "

    val extendsCode = genExtendsCode(parent, r.parents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(r.parentsCompanion)

    val code =
      s"""${genPackage(r)}
         |${genDoc(r.doc)}
         |final class ${r.name}($ctorParameters) $extendsCode {
         |  ${r.extra mkString EOL}
         |  ${genAlternativeConstructors(r.since, allFields) mkString EOL}
         |  ${genLazyMembers(r.fields) mkString EOL}
         |  ${genEquals(r, superFields)}
         |  ${genHashCode(r, superFields)}
         |  ${genToString(r, superFields, r.toStringImpl)}
         |  ${genCopy(r, allFields)}
         |  ${genWith(r, superFields)}
         |}
         |
         |object ${r.name}$extendsCodeCompanion {
         |  ${r.extraCompanion mkString EOL}
         |  ${genApplyOverloads(r, allFields) mkString EOL}
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateInterface(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    val allFields = superFields ++ i.fields

    val classDef = if (sealProtocols) "sealed abstract class" else "abstract class"
    val ctorParameters = genCtorParameters(i, allFields) mkString ", "
    val superCtorArguments = superFields map (_.name) mkString ", "

    val extendsCode = genExtendsCode(parent, i.parents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(i.parentsCompanion)

    val alternativeCtors = genAlternativeConstructors(i.since, allFields) mkString EOL
    val lazyMembers = genLazyMembers(i.fields) mkString EOL
    val messages = genMessages(i.messages) mkString EOL

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
         |  ${genToString(i, superFields, i.toStringBody)}
         |}
         |
         |object ${i.name}$extendsCodeCompanion {
         |  ${i.extraCompanion mkString EOL}
         |}""".stripMargin

    val childrenCode = i.children map (generate(s, _, Some(i), superFields ++ i.fields))
    ListMap(genFile(i) -> code) :: childrenCode reduce (_ merge _)
  }

  private def genDoc(doc: List[String]) = doc match {
    case Nil      => ""
    case l :: Nil => s"/** $l */"
    case lines =>
      val doc = lines map (l => s" * $l") mkString EOL
      s"""/**
         |$doc
         | */""".stripMargin
  }

  private def genExtendsCode(parent: Option[Interface], parents: List[String], superCtorArguments: String): String = {
    val parentInterface = parent.map(p => s"${fullyQualifiedName(p)}($superCtorArguments)")
    val allParents = parentInterface.toList ::: parents ::: List("Serializable")
    val extendsCode = allParents.mkString(" with ")
    if (extendsCode == "") "" else s"extends $extendsCode"
  }

  private def genExtendsCodeCompanion(companion: List[String]): String = {
    val extendsCodeCompanion = companion.mkString(" with ")
    if (extendsCodeCompanion == "") "" else s" extends $extendsCodeCompanion"
  }

  private def genParam(f: Field): String = s"${bq(f.name)}: ${genRealTpe(f.tpe, isParam = true)}"

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
        allFields map (f => s"(this.${bq(f.name)} == x.${bq(f.name)})") mkString " && "
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
        (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${bq(f.name)}.##)" }
      }

    s"""override def hashCode: Int = {
       |  $computationCode
       |}""".stripMargin
  }

  private def genToString(cl: ClassLike, superFields: List[Field], toString: List[String]) = {
    val body = if (toString.isEmpty) {
      val allFields = superFields ++ cl.fields
      if (allFields exists (_.tpe.lzy)) {
        s"super.toString // Avoid evaluating lazy members in toString to avoid circularity."
      } else if (allFields.isEmpty) {
        s""""${cl.name}()""""
      } else {
        val fieldsToString = allFields.map(f => bq(f.name)).mkString(" + ", """ + ", " + """, " + ")
        s""""${cl.name}("$fieldsToString")""""
      }
    } else toString mkString s"$EOL  "

    s"""override def toString: String = {
       |  $body
       |}""".stripMargin
  }

  private def genApplyOverloads(r: Record, allFields: List[Field]): List[String] =
    if (allFields.isEmpty) { // If there are no fields, we still need an `apply` method with an empty parameter list
      List(s"def apply(): ${r.name} = new ${r.name}()")
    } else {
      perVersionNumber(r.since, allFields) { (provided, byDefault) =>
        val applyParameters = provided map genParam mkString ", "

        val ctorCallArguments =
          allFields map {
            case f if provided contains f  => bq(f.name)
            case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
          } mkString ", "

        s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)"
      }
    }

  private def genAlternativeConstructors(since: VersionNumber, allFields: List[Field]) =
    perVersionNumber(since, allFields) {
      case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
        val ctorParameters = provided map genParam mkString ", "
        val thisCallArguments =
          allFields map {
            case f if provided contains f  => bq(f.name)
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
      case f if cl.fields.contains(f) && f.tpe.lzy => EOL + "_" + genParam(f)

      case f if cl.fields.contains(f) =>
        s"""$EOL${genDoc(f.doc)}
           |val ${genParam(f)}""".stripMargin

      case f => EOL + genParam(f)
    }

  private def genLazyMembers(fields: List[Field]): List[String] =
    fields filter (_.tpe.lzy) map { f =>
        s"""${genDoc(f.doc)}
           |lazy val ${bq(f.name)}: ${genRealTpe(f.tpe, isParam = false)} = _${f.name}""".stripMargin
    }

  private def genMessages(messages: List[Message]): List[String] =
    messages map { case Message(name, doc, responseTpe, request) =>
      val params = request map (a => s"${bq(a.name)}: ${genRealTpe(a.tpe, isParam = true)}") mkString ", "
      val argsDoc = request flatMap {
        case Request(_, Nil, _)           => Nil
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

  private def genPackage(d: Definition): String = d.namespace map (ns => s"package $ns") getOrElse ""

  private def genCopy(r: Record, allFields: List[Field]) = {
    def genParam(f: Field) = s"${bq(f.name)}: ${genRealTpe(f.tpe, isParam = true)} = ${bq(f.name)}"
    val params = allFields map genParam mkString ", "
    val constructorCall = allFields map (f => bq(f.name)) mkString ", "
    s"""protected[this] def copy($params): ${r.name} = {
       |  new ${r.name}($constructorCall)
       |}""".stripMargin
  }

  private def genWith(r: Record, superFields: List[Field]) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1) ; fst.toUpperCase + rst }
    val allFields = superFields ++ r.fields

    allFields map { f =>
      s"""def with${capitalize(f.name)}(${bq(f.name)}: ${genRealTpe(f.tpe, isParam = true)}): ${r.name} = {
         |  copy(${bq(f.name)} = ${bq(f.name)})
         |}""".stripMargin
    } mkString (EOL + EOL)
  }

}
