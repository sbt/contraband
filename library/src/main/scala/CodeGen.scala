package sbt.datatype
import scala.compat.Platform.EOL

abstract class CodeGenerator {

  def augmentIndentTrigger(s: String): Boolean
  def reduceIndentTrigger(s: String): Boolean

  protected class IndentationAwareBuffer(val indent: String, private var level: Int = 0) {
    private val buffer: StringBuilder = new StringBuilder

    private def append(s: String): Unit = {
      val clean = s.trim
      if (reduceIndentTrigger(clean)) level = 0 max (level - 1)
      buffer append (indent * level + clean + EOL)
      if (augmentIndentTrigger(clean)) level += 1
    }

    def +=(it: Iterator[String]): Unit = it foreach append
    def +=(s: String): Unit = s.lines foreach append

    override def toString(): String = buffer.mkString
  }

  protected def buffered(op: IndentationAwareBuffer => Unit): String

  protected final def perVersionNumber[T](allFields: List[Field])(op: (List[Field], List[Field]) => T): List[T] = {
    val versionNumbers = allFields.map(_.since).sorted.distinct
    versionNumbers map { v =>
      val (provided, byDefault) = allFields partition (_.since <= v)
      op(provided, byDefault)
    }
  }

  def generate(s: Schema): Map[String, String]
  final def generate(d: Definition, parent: Option[Protocol], superFields: List[Field]): Map[String, String] =
    d match {
      case p: Protocol    => generate(p, parent, superFields)
      case r: Record      => generate(r, parent, superFields)
      case e: Enumeration => generate(e)
    }
  def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[String, String]
  def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[String, String]
  def generate(e: Enumeration): Map[String, String]

}

object JavaCodeGen extends CodeGenerator {

  override def buffered(op: IndentationAwareBuffer => Unit): String = {
    val buffer = new IndentationAwareBuffer("\t")
    op(buffer)
    buffer.toString
  }

  override def augmentIndentTrigger(s: String) = s endsWith "{"
  override def reduceIndentTrigger(s: String) = s startsWith "}"

  override def generate(s: Schema): Map[String, String] =
    s.definitions flatMap (generate(_, None, Nil)) map {
      case (k, v) =>
        (k, buffered { b =>
          b += s"package ${s.namespace};"
          b += v.lines
        })

    } toMap

  override def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[String, String] = {
    val Protocol(name, doc, fields, children) = p
    val extendsCode = parent map (p => s"extends ${p.name}") getOrElse ""

    val code =
      s"""${genDoc(doc)}
         |public abstract class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(p, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genEquals(p, superFields)}
         |    ${genHashCode(p, superFields)}
         |    ${genToString(p, superFields)}
         |}""".stripMargin

    Map(genFileName(p) -> code) ++ (children flatMap (generate(_, Some(p), superFields ++ fields)))
  }

  override def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[String, String] = {
    val Record(name, doc, fields) = r
    val extendsCode = parent map (p => s"extends ${p.name}") getOrElse ""

    val code =
      s"""${genDoc(doc)}
         |public final class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(r, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genEquals(r, superFields)}
         |    ${genHashCode(r, superFields)}
         |    ${genToString(r, superFields)}
         |}""".stripMargin;

    Map(genFileName(r) -> code)
  }

  override def generate(e: Enumeration): Map[String, String] = {
    val Enumeration(name, doc, values) = e

    val valuesCode = values map { case EnumerationValue(name, doc) =>
      s"""${genDoc(doc)}
         |$name""".stripMargin
    } mkString ("," + EOL)

    val code =
      s"""${genDoc(doc)}
         |public enum $name {
         |    $valuesCode
         |}
         |""".stripMargin

    Map(genFileName(e) -> code)
  }

  private def genDoc(doc: Option[String]) = doc map (d => s"/** $d */") getOrElse ""

  private def genFileName(d: Definition) = d.name + ".java"

  private def genFields(fields: List[Field]) = fields map genField mkString EOL
  private def genField(f: Field) =
    s"""${genDoc(f.doc)}
       |private ${genRealTpe(f.tpe)} ${f.name};""".stripMargin

  private def genRealTpe(tpe: TpeRef): String = tpe match {
    case TpeRef(name, true, true)   => s"Lazy<$name[]>"
    case TpeRef(name, true, false)  => s"Lazy<$name>"
    case TpeRef(name, false, true)  => s"$name[]"
    case TpeRef(name, false, false) => name
  }

  private def genAccessors(fields: List[Field]) = fields map genAccessor mkString EOL
  private def genAccessor(field: Field) = {
    val accessCode =
      if (field.tpe.lzy) s"this.${field.name}.get()"
      else s"this.${field.name}"

    // We don't use `genRealTpe` here because the field will be evaluated
    // if it is lazy.
    val tpeSig =
      if (field.tpe.repeated) s"${field.tpe.name}[]"
      else field.tpe.name

    s"""public $tpeSig ${field.name}() {
       |    return $accessCode;
       |}""".stripMargin
  }

  private def genConstructors(cl: ClassLike, parent: Option[Protocol], superFields: List[Field]) =
    perVersionNumber(superFields ++ cl.fields) { (provided, byDefault) =>
      val ctorParameters = provided map (f => s"${genRealTpe(f.tpe)} _ ${f.name}") mkString ", "
      val superFieldsValues = superFields map {
        case f if provided contains f  => s"_${f.name}"
        case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
      }
      val superCall = superFieldsValues.mkString("super(", ", ", ");")
      val assignments = cl.fields map {
        case f if provided contains f  => s"${f.name} = _${f.name};"
        case f if byDefault contains f => f.default map (d => s"${f.name} = $d;") getOrElse sys.error(s"Need a default value for field ${f.name}.")
      } mkString EOL

      s"""public ${cl.name}($ctorParameters) {
         |    $superCall
         |    $assignments
         |}""".stripMargin
    } mkString (EOL + EOL)

  private def genEquals(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return this == obj; // We have lazy members, so use object identity to avoid circularity."
      } else {
        val comparisonCode =
          if (allFields.isEmpty) "return true;"
          else
            allFields.map {
              case f if f.tpe.repeated => s"java.util.Arrays.deepEquals(${f.name}(), o.${f.name}())"
              case f                   => s"${f.name}().equals(o.${f.name}())"
            }.mkString("return ", " && ", ";")

        s"""if (this == obj) {
           |    return true;
           |} else if (!(obj instanceof ${cl.name})) {
           |    return false;
           |} else {
           |    ${cl.name} o = (${cl.name})obj;
           |    $comparisonCode
           |}""".stripMargin
      }

    s"""public boolean equals(Object obj) {
       |    $body
       |}""".stripMargin
  }

  private def genHashCode(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return super.hashCode();"
      } else {
        val computation = (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${f.name}().hashCode())" }
        s"return $computation;"
      }

    s"""public int hashCode() {
       |    $body
       |}""".stripMargin
  }

  private def genToString(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val code =
      allFields.map{ f =>
        s""" + "${f.name}: " + ${f.name}()"""
      }.mkString(s""" "${cl.name}(" """, " + \", \"", " + \")\"")

    s"""public String toString() {
       |    return $code;
       |}""".stripMargin
  }

}

class ScalaCodeGen(genFileName: Definition => String) extends CodeGenerator {

  private implicit class MergeableMap[T](m: Map[T, String]) {
    def merge(o: Map[T, String]): Map[T, String] =
      (o foldLeft m) { case (acc, (k, v)) =>
        val existing = acc get k getOrElse ""
        acc + (k -> (existing + EOL + EOL + v))
      }
  }

  override def buffered(op: IndentationAwareBuffer => Unit): String = {
    val buffer = new IndentationAwareBuffer("  ")
    op(buffer)
    buffer.toString
  }

  override def augmentIndentTrigger(s: String) = s endsWith "{"
  override def reduceIndentTrigger(s: String) = s startsWith "}"

  private def genDoc(doc: Option[String]) = doc map (d => s"/** $d */") getOrElse ""

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
      perVersionNumber(allFields) {
        case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
          val ctorParameters =
            provided map {
              case f if f.tpe.lzy => s"${f.name}: => ${genRealTpe(f.tpe)}"
              case f              => s"${f.name}: ${genRealTpe(f.tpe)}"
            } mkString ", "
          val thisCallArguments =
            allFields map {
              case f if provided contains f   => f.name
              case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for fied ${f.name}.")
            } mkString ", "

          s"def this($ctorParameters) = this($thisCallArguments)"
        case (_, _) => ""
      } mkString EOL

    val applyOverloads =
      perVersionNumber(allFields) { (provided, byDefault) =>
        val applyParameters =
          provided map {
            case f if f.tpe.lzy => s"${f.name}: => ${genRealTpe(f.tpe)}"
            case f              => s"${f.name}: ${genRealTpe(f.tpe)}"
          } mkString ", "
        val ctorCallArguments = allFields map {
          case f if provided contains f  => f.name
          case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for field ${f.name}.")
        } mkString ", "

        s"def apply($applyParameters): ${r.name} = new ${r.name}($ctorCallArguments)"
      } mkString EOL

    val ctorParameters =
      allFields map {
        case f if r.fields.contains(f)    && f.tpe.lzy => s"_${f.name}: => ${genRealTpe(f.tpe)}"
        case f if r.fields.contains(f)                 => s"val ${f.name}: ${genRealTpe(f.tpe)}"
        case f if superFields.contains(f) && f.tpe.lzy => s"${f.name}: => ${genRealTpe(f.tpe)}"
        case f if superFields.contains(f)              => s"${f.name}: ${genRealTpe(f.tpe)}"
      } mkString ", "
    val superCtorArguments =
      superFields map (_.name) mkString ", "
    val extendsCode =
      parent map (p => s"extends ${p.name}($superCtorArguments)") getOrElse ""
    val lazyMembers =
      r.fields filter (_.tpe.lzy) map (f => s"lazy val ${f.name}: ${genRealTpe(f.tpe)} = _${f.name}") mkString EOL

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
      perVersionNumber(allFields) {
        case (provided, byDefault) if byDefault.nonEmpty => // Don't duplicate up-to-date constructor
          val ctorParameters =
            provided map {
              case f if f.tpe.lzy => s"${f.name}: => ${genRealTpe(f.tpe)}"
              case f              => s"${f.name}: ${genRealTpe(f.tpe)}"
            } mkString ", "
          val thisCallArguments =
            allFields map {
              case f if provided contains f   => f.name
              case f if byDefault contains f => f.default getOrElse sys.error(s"Need a default value for fied ${f.name}.")
            } mkString ", "

          s"def this($ctorParameters) = this($thisCallArguments)"

        case (_, _) => ""
      } mkString EOL

    val ctorParameters =
      allFields map {
        case f if p.fields.contains(f)    && f.tpe.lzy => s"_${f.name}: => ${genRealTpe(f.tpe)}"
        case f if p.fields.contains(f)                 => s"val ${f.name}: ${genRealTpe(f.tpe)}"
        case f if superFields.contains(f) && f.tpe.lzy => s"${f.name}: => ${genRealTpe(f.tpe)}"
        case f if superFields.contains(f)              => s"${f.name}: ${genRealTpe(f.tpe)}"
      } mkString ", "

    val superCtorArguments =
      superFields map (_.name) mkString ", "

    val extendsCode =
      parent map (p => s"extends ${p.name}($superCtorArguments)") getOrElse ""

    val lazyMembers =
      p.fields filter (_.tpe.lzy) map (f => s"lazy val ${f.name}: ${genRealTpe(f.tpe)} = _${f.name}") mkString EOL

    val code =
      s"""${genDoc(p.doc)}
         |sealed abstract class ${p.name}($ctorParameters) {
         |  $alternativeCtors
         |  $lazyMembers
         |  ${genEquals(p, superFields)}
         |  ${genHashCode(p, superFields)}
         |  ${genToString(p, superFields)}
         |}""".stripMargin

    Map(genFileName(p) -> code) :: (p.children map (generate(_, Some(p), superFields ++ p.fields))) reduce (_ merge _)
  }

  private def genRealTpe(tpe: TpeRef) = tpe match {
    case TpeRef(name, false, false) => name
    case TpeRef(name, false, true)  => s"Array[$name]"
    case TpeRef(name, true, false)  => name
    case TpeRef(name, true, true)   => s"Array[$name]"
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

}

object CodeGen {
  def generate(ps: ProtocolSchema): String =
    {
      val ns = ps.namespace
      val types = ps.types map { tpe: TypeDef => generateType(ns, tpe) }
      val typesCode = types.mkString("\n")
      s"""package $ns

$typesCode"""
    }

  def generateType(namespace: String, td: TypeDef): String =
    {
      val name = td.name
      val fields = td.fields map { field: FieldSchema => s"""${field.name}: ${field.`type`.name}""" }
      val fieldsCode = fields.mkString(",\n  ")
      val ctorFields = td.fields map { field: FieldSchema => s"""val ${field.name}: ${field.`type`.name}""" }
      val ctorFieldsCode = ctorFields.mkString(",\n  ")
      val fieldNames = td.fields map { field: FieldSchema => field.name }
      val sinces = (td.fields map {_.since}).distinct.sorted
      val inclusives = sinces.zipWithIndex map { case (k, idx) =>
        val dropNum = sinces.size - 1 - idx
        sinces dropRight dropNum
      }
      val alternatives = inclusives dropRight 1
      val altCode = (alternatives map { alts =>
        generateAltCtor(td.fields, alts)
      }).mkString("\n    ")

      val fieldNamesCode = fieldNames.mkString(", ")
      val mainApply =
        s"""def apply($fieldsCode): $name =
    new $name($fieldNamesCode)"""
      s"""final class $name($ctorFieldsCode) {
  ${altCode}
  ${generateEquals(name, fieldNames)}
  ${generateHashCode(fieldNames)}
  ${generateCopy(name, td.fields)}
}

object $name {
  $mainApply
}"""
    }
  
  def generateAltCtor(fields: Vector[FieldSchema], versions: Vector[VersionNumber]): String =
    {
      val vs = versions.toSet
      val params = fields filter { f => vs contains f.since } map { f => s"${f.name}: ${f.`type`.name}" }
      val paramsCode = params.mkString(", ")
      val args = fields map { f =>
        if (vs contains f.since) f.name
        else quote(f.defaultValue getOrElse { sys.error(s"${f.name} is missing `default` value") },
          f.`type`.name)
      }
      val argsCode = args.mkString(", ")
      s"def this($paramsCode) = this($argsCode)"
    }
  def quote(value: String, tpe: String): String =
    tpe match {
      case "String" => s""""$value"""" // "
      case _        => value
    }

  def generateCopy(name: String, fields: Vector[FieldSchema]): String =
    {
      val params = fields map { f => s"${f.name}: ${f.`type`.name} = this.${f.name}" }
      val paramsCode = params.mkString(",\n    ")
      val args = fields map { f => f.name }
      val argsCode = args.mkString(", ")
      s"private[this] def copy($paramsCode): $name =\n" +
      s"    new $name($argsCode)"
    }

  def generateEquals(name: String, fieldNames: Vector[String]): String =
    {
      val fieldNamesEq = fieldNames map { n: String => s"(this.$n == x.$n)" }
      val fieldNamesEqCode = fieldNamesEq.mkString(" &&\n        ")
      s"""override def equals(o: Any): Boolean =
    o match {
      case x: $name =>
        $fieldNamesEqCode
      case _ => false
    }"""
    }

  def generateHashCode(fieldNames: Vector[String]): String =
    {
      val fieldNamesHash = fieldNames map { n: String => 
        s"hash = hash * 31 + this.$n.##"
      }
      val fieldNameHashCode = fieldNamesHash.mkString("\n      ") 
      s"""override def hashCode: Int =
    {
      var hash = 1
      $fieldNameHashCode
      hash
    }"""
    }
}

