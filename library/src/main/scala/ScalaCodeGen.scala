package sbt.datatype
import scala.compat.Platform.EOL

/**
 * Code generator for Scala.
 */
class ScalaCodeGen(genFileName: Definition => String) extends CodeGenerator {

  private implicit class MergeableMap[T](m: Map[T, String]) {
    def merge(o: Map[T, String]): Map[T, String] =
      (o foldLeft m) { case (acc, (k, v)) =>
        val existing = acc get k getOrElse ""
        acc + (k -> (existing + EOL + EOL + v))
      }
  }

  override def augmentIndentTrigger(s: String) = s endsWith "{"
  override def reduceIndentTrigger(s: String) = s startsWith "}"
  override def buffered(op: IndentationAwareBuffer => Unit): String = {
    val buffer = new IndentationAwareBuffer("  ")
    op(buffer)
    buffer.toString
  }

  override def generate(s: Schema): Map[String,String] = {
    s.definitions map (generate (_, None, Nil)) reduce (_ merge _) map {
      case (k, v) =>
        (k, buffered { b =>
          b += s"package ${s.namespace}"
          b +=  ""
          b +=  v.lines
        })
    }
  }

  override def generate(e: Enumeration): Map[String,String] = {
    val values =
      e.values map { case (EnumerationValue(name, doc)) =>
        s"""${genDoc(doc)}
           |case object $name extends ${e.name}""".stripMargin
      } mkString EOL

    val code =
      s"""${genDoc(e.doc)}
         |sealed abstract class ${e.name}
         |object ${e.name} {
         |  $values
         |}""".stripMargin

    Map(genFileName(e) -> code)
  }

  override def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[String, String] = {
    val allFields = superFields ++ r.fields

    val alternativeCtors =
      genAlternativeConstructors(allFields) mkString EOL

    val applyOverloads =
      perVersionNumber(allFields) { (provided, byDefault) =>
        val applyParameters =
          provided map genParam mkString ", "

        val ctorCallArguments =
          allFields map {
            case f if provided contains f  => f.name
            case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
          } mkString ", "

        s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)"
      } mkString EOL

    val ctorParameters =
      genCtorParameters(r, allFields) mkString ", "

    val superCtorArguments = superFields map (_.name) mkString ", "

    val extendsCode =
      parent map (p => s"extends ${p.name}($superCtorArguments)") getOrElse ""

    val lazyMembers =
      r.fields filter (_.tpe.lzy) map (f => s"lazy val ${f.name}: ${genRealTpe(f.tpe, isParam = false)} = _${f.name}") mkString EOL

    val code =
      s"""${genDoc(r.doc)}
         |final class ${r.name}($ctorParameters) $extendsCode {
         |  $alternativeCtors
         |  $lazyMembers
         |  ${genEquals(r, superFields)}
         |  ${genHashCode(r, superFields)}
         |  ${genToString(r, superFields)}
         |}
         |
         |object ${r.name} {
         |  $applyOverloads
         |}""".stripMargin

    Map(genFileName(r) -> code)
  }

  override def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[String,String] = {
    val allFields = superFields ++ p.fields

    val alternativeCtors =
      genAlternativeConstructors(allFields) mkString EOL

    val ctorParameters =
      genCtorParameters(p, allFields) mkString ", "

    val superCtorArguments =
      superFields map (_.name) mkString ", "

    val extendsCode =
      parent map (p => s"extends ${p.name}($superCtorArguments)") getOrElse ""

    val lazyMembers =
      p.fields filter (_.tpe.lzy) map (f => s"lazy val ${f.name}: ${genRealTpe(f.tpe, isParam = false)} = _${f.name}") mkString EOL

    val code =
      s"""${genDoc(p.doc)}
         |sealed abstract class ${p.name}($ctorParameters) $extendsCode {
         |  $alternativeCtors
         |  $lazyMembers
         |  ${genEquals(p, superFields)}
         |  ${genHashCode(p, superFields)}
         |  ${genToString(p, superFields)}
         |}""".stripMargin

    Map(genFileName(p) -> code) :: (p.children map (generate(_, Some(p), superFields ++ p.fields))) reduce (_ merge _)
  }

  private def genDoc(doc: Option[String]) = doc map (d => s"/** $d */") getOrElse ""

  private def genParam(f: Field): String = s"${f.name}: ${genRealTpe(f.tpe, isParam = true)}"

  private def genRealTpe(tpe: TpeRef, isParam: Boolean) = {
    val base = if (tpe.repeated) s"Array[${tpe.name}]" else tpe.name
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
        s"super.hashCode"
      } else {
        (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${f.name}.##)" }
      }

    s"""override def hashCode: Int = {
       |  $computationCode
       |}""".stripMargin
  }

  private def genToString(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val fieldsToString =
      allFields.map(_.name).mkString(" + ", """ + ", " + """, " + ")

    s"""override def toString: String = {
       |  "${cl.name}("$fieldsToString")"
       |}""".stripMargin
  }

  private def genAlternativeConstructors(allFields: List[Field]) =
    perVersionNumber(allFields) {
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
      case f if cl.fields.contains(f) && f.tpe.lzy => "_" + genParam(f)
      case f if cl.fields.contains(f)              => "val " + genParam(f)
      case f                                       => genParam(f)
    }

}
