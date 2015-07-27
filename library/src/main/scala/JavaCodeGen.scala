package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * Code generator for Java.
 */
object JavaCodeGen extends CodeGenerator {

  override protected def augmentIndentAfterTrigger(s: String) = s endsWith "{"
  override protected def reduceIndentTrigger(s: String) = s startsWith "}"
  override protected def buffered(op: IndentationAwareBuffer => Unit): String = {
    val buffer = new IndentationAwareBuffer("    ")
    op(buffer)
    buffer.toString
  }

  override def generate(s: Schema): Map[File, String] = {
    def makeFile(name: String) =
      s.namespace map (ns => new File(ns.replace(".", File.separator), name)) getOrElse new File(name)

    s.definitions flatMap (generate(_, None, Nil)) map {
      case (k, v) =>
        (makeFile(k), buffered { b =>
          b += s.namespace map (ns => s"package $ns;") getOrElse ""
          b += v.lines
        })

    } toMap
  }

  override protected def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[String, String] = {
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

  override protected def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[String, String] = {
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
         |}""".stripMargin

    Map(genFileName(r) -> code)
  }

  override protected def generate(e: Enumeration): Map[String, String] = {
    val Enumeration(name, doc, values) = e

    val valuesCode = values map { case EnumerationValue(name, doc) =>
      s"""${genDoc(doc)}
         |$name""".stripMargin
    } mkString ("," + EOL)

    val code =
      s"""${genDoc(doc)}
         |public enum $name {
         |    $valuesCode
         |}""".stripMargin

    Map(genFileName(e) -> code)
  }

  private def genDoc(doc: Option[String]) = doc map (d => s"/** $d */") getOrElse ""

  private def genFileName(d: Definition) = d.name + ".java"

  private def genFields(fields: List[Field]) = fields map genField mkString EOL
  private def genField(f: Field) =
    s"""${genDoc(f.doc)}
       |private ${genRealTpe(f.tpe)} ${f.name};""".stripMargin

  def isPrimitive(tpe: TpeRef) = !tpe.repeated && ! tpe.lzy && tpe.name != boxedType(tpe.name)

  private def boxedType(tpe: String): String = tpe match {
    case "boolean" => "Boolean"
    case "byte"    => "Byte"
    case "char"    => "Character"
    case "float"   => "Float"
    case "int"     => "Integer"
    case "long"    => "Long"
    case "short"   => "Short"
    case "double"  => "Double"
    case other     => other
  }

  private def genRealTpe(tpe: TpeRef): String = tpe match {
    case TpeRef(name, true, true)   => s"Lazy<$name[]>"
    case TpeRef(name, true, false)  => s"Lazy<${boxedType(name)}>"
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
      val ctorParameters = provided map (f => s"${genRealTpe(f.tpe)} _${f.name}") mkString ", "
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
              case f if f.tpe.repeated =>     s"java.util.Arrays.deepEquals(${f.name}(), o.${f.name}())"
              case f if isPrimitive(f.tpe) => s"(${f.name}() == o.${f.name}())"
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

  private def hashCode(f: Field): String =
    if (isPrimitive(f.tpe)) s"(new ${boxedType(f.tpe.name)}(${f.name})).hashCode()"
    else s"${f.name}().hashCode()"

  private def genHashCode(cl: ClassLike, superFields: List[Field]) = {
    val allFields = superFields ++ cl.fields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return super.hashCode();"
      } else {
        val computation = (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${hashCode(f)})" }
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
