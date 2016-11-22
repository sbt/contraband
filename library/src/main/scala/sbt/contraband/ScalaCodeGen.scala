package sbt.contraband

import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap
import CodeGen.bq
import ast.{ Definition => _, _ }
import AstUtil._

/**
 * Code generator for Scala.
 */
class ScalaCodeGen(scalaArray: String, genFile: Any => File, sealProtocols: Boolean) extends CodeGenerator {

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


  override def generate(s: Document): ListMap[File, String] =
    (s.definitions collect {
      case td: TypeDefinition => td
    }) map (generate (s, _)) reduce (_ merge _) mapV (_.indented)

  override def generateEnum(s: Document, e: EnumTypeDefinition): ListMap[File, String] = {
    val values =
      e.values map { case (EnumValueDefinition(name, _, comments, _)) =>
        s"""${genDoc(toDoc(comments))}
           |case object $name extends ${e.name}""".stripMargin
      } mkString EOL

    val extra: List[String] = toExtra(e)
    val code =
      s"""${genPackage(e)}
         |${genDoc(toDoc(e.comments))}
         |sealed abstract class ${e.name} extends Serializable
         |object ${e.name} {
         |  ${extra mkString EOL}
         |  $values
         |}""".stripMargin

    ListMap(genFile(e) -> code)
  }

  override def generateRecord(s: Document, r: ObjectTypeDefinition): ListMap[File, String] = {
    // println(s"generateRecord: ${r.name}")
    val allFields = r.fields filter { _.arguments.isEmpty }
    val extraParents = toExtraIntf(r)
    val parents = r.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    // println(s"parents: $parents,  parent: $parent")
    val ctorParameters = genCtorParameters(r, parent) mkString ","
    val superFields = (parent map { _.fields }).toList.flatten
    val superCtorArguments = superFields map (_.name) mkString ", "
    val extendsCode = genExtendsCode(parent, extraParents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(toCompanionExtraIntfComment(r))
    val companionExtra: List[String] = toCompanionExtra(r)
    val toStringImpl: List[String] = toToStringImpl(r)
    val lazyMembers = genLazyMembers(localFields(r, parentsInSchema)) mkString EOL

    val doc = toDoc(r.comments)
    val extra = toExtra(r)
    val since = getSince(r.directives)
    val code =
      s"""${genPackage(r)}
         |${genDoc(doc)}
         |final class ${r.name}($ctorParameters) $extendsCode {
         |  ${extra mkString EOL}
         |  ${genAlternativeConstructors(since, allFields) mkString EOL}
         |  ${lazyMembers}
         |  ${genEquals(r)}
         |  ${genHashCode(r)}
         |  ${genToString(r, toStringImpl)}
         |  ${genCopy(r)}
         |  ${genWith(r)}
         |}
         |
         |object ${r.name}$extendsCodeCompanion {
         |  ${companionExtra mkString EOL}
         |  ${genApplyOverloads(r, allFields) mkString EOL}
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateInterface(s: Document, i: InterfaceTypeDefinition): ListMap[File, String] = {
    val extraParents = toExtraIntf(i)
    val parents = i.interfaces
    val parentsInSchema = lookupInterfaces(s, parents)
    val parent: Option[InterfaceTypeDefinition] = parentsInSchema.headOption
    val allFields = i.fields filter { _.arguments.isEmpty }
    val classDef = if (sealProtocols) "sealed abstract class" else "abstract class"
    val ctorParameters = genCtorParameters(i, parent) mkString ", "
    val superCtorArguments =
      parent match {
        case Some(x) => x.fields map (_.name) mkString ", "
        case _       => ""
      }
    val extendsCode = genExtendsCode(parent, extraParents, superCtorArguments)
    val extendsCodeCompanion = genExtendsCodeCompanion(toCompanionExtraIntfComment(i))
    val doc = toDoc(i.comments)
    val extra = toExtra(i)
    val since = getSince(i.directives)
    val alternativeCtors = genAlternativeConstructors(since, allFields) mkString EOL
    val lazyMembers = genLazyMembers(localFields(i, parentsInSchema)) mkString EOL
    val msgs = i.fields filter { _.arguments.nonEmpty }
    val messages = genMessages(msgs) mkString EOL
    val toStringImpl: List[String] = toToStringImpl(i)
    val companionExtra: List[String] = toCompanionExtra(i)

    val code =
      s"""${genPackage(i)}
         |${genDoc(doc)}
         |$classDef ${i.name}($ctorParameters) $extendsCode {
         |  ${extra mkString EOL}
         |  $alternativeCtors
         |  $lazyMembers
         |  $messages
         |  ${genEquals(i)}
         |  ${genHashCode(i)}
         |  ${genToString(i, toStringImpl)}
         |}
         |
         |object ${i.name}$extendsCodeCompanion {
         |  ${companionExtra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(i) -> code)
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

  private def genExtendsCode(parent: Option[InterfaceTypeDefinition], extraParents: List[String], superCtorArguments: String): String = {
    val parentInterface = parent.map(p => s"${fullyQualifiedName(p)}($superCtorArguments)")
    val allParents = parentInterface.toList ::: extraParents ::: List("Serializable")
    val extendsCode = allParents.mkString(" with ")
    if (extendsCode == "") "" else s"extends $extendsCode"
  }

  private def genExtendsCodeCompanion(companion: List[String]): String = {
    val extendsCodeCompanion = companion.mkString(" with ")
    if (extendsCodeCompanion == "") "" else s" extends $extendsCodeCompanion"
  }

  private def genParam(f: FieldDefinition): String = s"${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = true)}"

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

  private def genRealTpe(tpe: ast.Type, isParam: Boolean) = {
    val scalaTpe = lookupTpe(tpe.name)
    val base = tpe match {
      case x if x.isListType      => s"$scalaArray[$scalaTpe]"
      case x if !x.isNotNullType  => s"Option[$scalaTpe]"
      case _                      => scalaTpe
    }
    if (tpe.isLazyType && isParam) s"=> $base" else base
  }

  private def genEquals(cl: RecordLikeDefinition) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val comparisonCode =
      if (allFields exists (_.fieldType.isLazyType)) {
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

  private def genHashCode(cl: RecordLikeDefinition) = {
    val allFields = cl.fields filter { _.arguments.isEmpty }
    val computationCode =
      if (allFields exists (_.fieldType.isLazyType)) {
        s"super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity."
      } else {
        (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${bq(f.name)}.##)" }
      }

    s"""override def hashCode: Int = {
       |  $computationCode
       |}""".stripMargin
  }

  private def genToString(cl: RecordLikeDefinition, toStringImpl: List[String]) = {
    val body = if (toStringImpl.isEmpty) {
      val allFields = cl.fields filter { _.arguments.isEmpty }
      if (allFields exists (_.fieldType.isLazyType)) {
        s"super.toString // Avoid evaluating lazy members in toString to avoid circularity."
      } else if (allFields.isEmpty) {
        s""""${cl.name}()""""
      } else {
        val fieldsToString = allFields.map(f => bq(f.name)).mkString(" + ", """ + ", " + """, " + ")
        s""""${cl.name}("$fieldsToString")""""
      }
    } else toStringImpl mkString s"$EOL  "

    s"""override def toString: String = {
       |  $body
       |}""".stripMargin
  }

  private def renderScalaValue(v: Value, tpe: Type): String =
    v match {
      case x: NullValue =>
        if (!tpe.isNotNullType) "None"
        else if (tpe.isListType) "Vector()"
        else sys.error(s"Expected $tpe but found $v")
      case x: ScalarValue =>
        if (tpe.isListType) "Vector(${x.renderPretty})"
        else if (tpe.isNotNullType) x.renderPretty
        else s"Some(${x.renderPretty})"
      case _ => v.renderPretty
    }

  private def genApplyOverloads(r: ObjectTypeDefinition, allFields: List[FieldDefinition]): List[String] =
    if (allFields.isEmpty) { // If there are no fields, we still need an `apply` method with an empty parameter list
      List(s"def apply(): ${r.name} = new ${r.name}()")
    } else {
      val since = getSince(r.directives)
      perVersionNumber(since, allFields) { (provided, byDefault) =>
        val applyParameters = provided map genParam mkString ", "

        val ctorCallArguments =
          allFields map {
            case f if provided contains f  => bq(f.name)
            case f if byDefault contains f =>
              f.defaultValue match {
                case Some(v) => renderScalaValue(v, f.fieldType)
                case _       => sys.error(s"Need a default value for field ${f.name}.")
              }
          } mkString ", "

        s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)"
      }
    }

  private def genAlternativeConstructors(since: VersionNumber, allFields: List[FieldDefinition]) =
    perVersionNumber(since, allFields) {
      case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
        val ctorParameters = provided map genParam mkString ", "
        val thisCallArguments =
          allFields map {
            case f if provided contains f  => bq(f.name)
            case f if byDefault contains f =>
              f.defaultValue match {
                case Some(v) => renderScalaValue(v, f.fieldType)
                case _       => sys.error(s"Need a default value for field ${f.name}.")
              }
          } mkString ", "

        s"def this($ctorParameters) = this($thisCallArguments)"

      case _ => ""
    }

  // If a class has a lazy member, it means that the class constructor will have a call-by-name
  // parameter. Because val parameters may not be call-by-name, we prefix the parameter with `_`
  // and we will create the actual lazy val as a regular class member.
  // Non-lazy fields that belong to `cl` are made val parameters.
  private def genCtorParameters(cl: RecordLikeDefinition, parent: Option[InterfaceTypeDefinition]): List[String] =
    {
      val allFields = cl.fields filter { _.arguments.isEmpty }
      val parentFields: List[FieldDefinition] =
        parent match {
          case Some(x) => x.fields filter { _.arguments.isEmpty }
          case _       => Nil
        }
      def inParent(f: FieldDefinition): Boolean = {
        val x = parentFields exists { _.name == f.name }
        x
      }
      allFields map {
        case f if !inParent(f) && f.fieldType.isLazyType =>
          EOL + "_" + genParam(f)
        case f if !inParent(f) =>
          val doc = toDoc(f.comments)
          s"""$EOL${genDoc(doc)}
             |val ${genParam(f)}""".stripMargin
        case f => EOL + genParam(f)
      }
    }

  private def genLazyMembers(fields: List[FieldDefinition]): List[String] =
    fields filter (_.fieldType.isLazyType) map { f =>
        val doc = toDoc(f.comments)
        s"""${genDoc(doc)}
           |lazy val ${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = false)} = _${f.name}""".stripMargin
    }

  private def genMessages(messages: List[FieldDefinition]): List[String] =
    messages map { case FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) =>
      val params = arguments map (a => s"${bq(a.name)}: ${genRealTpe(a.valueType, isParam = true)}") mkString ", "
      val argsDoc = arguments flatMap { a: InputValueDefinition =>
        toDoc(a.comments) match {
          case Nil        => Nil
          case doc :: Nil => s"@param ${a.name} $doc" :: Nil
          case docs =>
            val prefix = s"@param ${a.name} "
            docs.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
        }
      }
      val doc = toDoc(comments)
      s"""${genDoc(doc ++ argsDoc)}
         |def $name($params): ${genRealTpe(fieldType, isParam = false)}"""
    }

  private def fullyQualifiedName(d: TypeDefinition): String = {
    val path = d.namespace map (ns => ns + ".") getOrElse ""
    path + d.name
  }

  private def genPackage(d: TypeDefinition): String = d.namespace map (ns => s"package $ns") getOrElse ""

  private def genCopy(r: ObjectTypeDefinition) = {
    val allFields = r.fields filter { _.arguments.isEmpty }
    def genParam(f: FieldDefinition) = s"${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = true)} = ${bq(f.name)}"
    val params = allFields map genParam mkString ", "
    val constructorCall = allFields map (f => bq(f.name)) mkString ", "
    s"""protected[this] def copy($params): ${r.name} = {
       |  new ${r.name}($constructorCall)
       |}""".stripMargin
  }

  private def genWith(r: ObjectTypeDefinition) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1) ; fst.toUpperCase + rst }
    r.fields map { f =>
      s"""def with${capitalize(f.name)}(${bq(f.name)}: ${genRealTpe(f.fieldType, isParam = true)}): ${r.name} = {
         |  copy(${bq(f.name)} = ${bq(f.name)})
         |}""".stripMargin
    } mkString (EOL + EOL)
  }

}