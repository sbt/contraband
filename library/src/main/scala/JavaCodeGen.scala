package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * Code generator for Java.
 */
object JavaCodeGen extends CodeGenerator {

  /** Indentation configuration for Java sources. */
  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "    "
    override def augmentIndentAfterTrigger(s: String) = s endsWith "{"
    override def reduceIndentTrigger(s: String) = s startsWith "}"
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Schema): Map[File, String] =
    s.definitions flatMap (generate(_, None, Nil) mapValues (_.indented)) toMap

  override def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    val Protocol(name, _, namespace, _, doc, fields, children) = p
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"

    val code =
      s"""${genPackage(p)}
         |${genDoc(doc)}
         |public abstract class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(p, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genEquals(p, superFields)}
         |    ${genHashCode(p, superFields)}
         |    ${genToString(p, superFields)}
         |}""".stripMargin

    Map(genFile(p) -> code) ++ (children flatMap (generate(_, Some(p), fields ++ superFields)))
  }

  override def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    val Record(name, _, namespace, _, doc, fields) = r
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"

    val code =
      s"""${genPackage(r)}
         |${genDoc(doc)}
         |public final class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(r, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genEquals(r, superFields)}
         |    ${genHashCode(r, superFields)}
         |    ${genToString(r, superFields)}
         |}""".stripMargin

    Map(genFile(r) -> code)
  }

  override def generate(e: Enumeration): Map[File, String] = {
    val Enumeration(name, _, namespace, _, doc, values) = e

    val valuesCode = values map { case EnumerationValue(name, doc) =>
      s"""${genDoc(doc)}
         |$name""".stripMargin
    } mkString ("," + EOL)

    val code =
      s"""${genPackage(e)}
         |${genDoc(doc)}
         |public enum $name {
         |    $valuesCode
         |}""".stripMargin

    Map(genFile(e) -> code)
  }

  private def genDoc(doc: Option[List[String]]) = doc map {
    case l :: Nil => s"/** $l */"
    case lines =>
      val doc = lines map (l => s" * $l") mkString EOL
      s"""/**
         |$doc
         | */""".stripMargin
  } getOrElse ""

  private def genFile(d: Definition) = {
    val fileName = d.name + ".java"
    d.namespace map (ns => new File(ns.replace(".", File.separator), fileName)) getOrElse new File(fileName)
  }

  private def genFields(fields: List[Field]) = fields map genField mkString EOL
  private def genField(f: Field) =
    s"""${genDoc(f.doc)}
       |private ${genRealTpe(f.tpe)} ${f.name};""".stripMargin

  private def isPrimitive(tpe: TpeRef) = !tpe.repeated && !tpe.lzy && tpe.name != boxedType(tpe.name)
  private def isPrimitiveArray(tpe: TpeRef) = tpe.repeated && !tpe.lzy && tpe.name != boxedType(tpe.name)

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
    perVersionNumber(cl.since, cl.fields ++ superFields) { (provided, byDefault) =>
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
    val allFields = cl.fields ++ superFields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return this == obj; // We have lazy members, so use object identity to avoid circularity."
      } else {
        val comparisonCode =
          if (allFields.isEmpty) "return true;"
          else
            allFields.map {
              case f if isPrimitive(f.tpe)      => s"(${f.name}() == o.${f.name}())"
              case f if isPrimitiveArray(f.tpe) => s"java.util.Arrays.equals(${f.name}(), o.${f.name}())"
              case f if f.tpe.repeated          => s"java.util.Arrays.deepEquals(${f.name}(), o.${f.name}())"
              case f                            => s"${f.name}().equals(o.${f.name}())"
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
    if (isPrimitive(f.tpe)) s"(new ${boxedType(f.tpe.name)}(${f.name}())).hashCode()"
    else s"${f.name}().hashCode()"

  private def genHashCode(cl: ClassLike, superFields: List[Field]) = {
    val allFields = cl.fields ++ superFields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity."
      } else {
        val computation = (allFields foldLeft ("17")) { (acc, f) => s"37 * ($acc + ${hashCode(f)})" }
        s"return $computation;"
      }

    s"""public int hashCode() {
       |    $body
       |}""".stripMargin
  }

  private def genToString(cl: ClassLike, superFields: List[Field]) = {
    val allFields = cl.fields ++ superFields
    val body =
      if (allFields exists (_.tpe.lzy)) {
        "return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity."
      } else {
        allFields.map{ f =>
          s""" + "${f.name}: " + ${f.name}()"""
        }.mkString(s"""return "${cl.name}(" """, " + \", \"", " + \")\";")
      }

    s"""public String toString() {
       |    $body
       |}""".stripMargin
  }

  private def genPackage(d: Definition) =
    d.namespace map (ns => s"package $ns;") getOrElse ""

  private def fullyQualifiedName(d: Definition) = {
    val path = d.namespace map (ns => ns + ".") getOrElse ""
    path + d.name
  }

}
