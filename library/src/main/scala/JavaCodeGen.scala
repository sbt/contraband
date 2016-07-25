package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap

/**
 * Code generator for Java.
 */
class JavaCodeGen(lazyInterface: String, optionalInterface: String) extends CodeGenerator {

  /** Indentation configuration for Java sources. */
  implicit object indentationConfiguration extends IndentationConfiguration {
    override val indentElement = "    "
    override def augmentIndentAfterTrigger(s: String) = s endsWith "{"
    override def reduceIndentTrigger(s: String) = s startsWith "}"
    override def enterMultilineJavadoc(s: String) = s == "/**"
    override def exitMultilineJavadoc(s: String) = s == "*/"
  }

  override def generate(s: Schema): ListMap[File, String] =
    ListMap(s.definitions.toList flatMap (generate(s, _, None, Nil).toList): _*) mapV (_.indented)

  override def generateInterface(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    val Interface(name, _, namespace, _, doc, fields, messages, children, extra) = i
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"

    val code =
      s"""${genPackage(i)}
         |${genDoc(doc)}
         |public abstract class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(i, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genMessages(messages)}
         |    ${genEquals(i, superFields)}
         |    ${genHashCode(i, superFields)}
         |    ${genToString(i, superFields)}
         |    ${extra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(i) -> code) ++ (children flatMap (generate(s, _, Some(i), superFields ++ fields)))
  }

  override def generateRecord(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    val Record(name, _, namespace, _, doc, fields, extra) = r
    val extendsCode = parent map (p => s"extends ${fullyQualifiedName(p)}") getOrElse "implements java.io.Serializable"

    val code =
      s"""${genPackage(r)}
         |${genDoc(doc)}
         |public final class $name $extendsCode {
         |    ${genFields(fields)}
         |    ${genConstructors(r, parent, superFields)}
         |    ${genAccessors(fields)}
         |    ${genWith(r, superFields)}
         |    ${genEquals(r, superFields)}
         |    ${genHashCode(r, superFields)}
         |    ${genToString(r, superFields)}
         |    ${extra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(r) -> code)
  }

  override def generateEnum(s: Schema, e: Enumeration): ListMap[File, String] = {
    val Enumeration(name, _, namespace, _, doc, values, extra) = e

    val valuesCode = values map { case EnumerationValue(name, doc) =>
      s"""${genDoc(doc)}
         |$name""".stripMargin
    } mkString ("," + EOL)

    val code =
      s"""${genPackage(e)}
         |${genDoc(doc)}
         |public enum $name {
         |    $valuesCode
         |    ${extra mkString EOL}
         |}""".stripMargin

    ListMap(genFile(e) -> code)
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
    case TpeRef(name, true, true, _)       => s"$lazyInterface<$name[]>"
    case TpeRef(name, true, false, true)   => s"$lazyInterface<$optionalInterface<${boxedType(name)}>>"
    case TpeRef(name, true, false, false)  => s"$lazyInterface<${boxedType(name)}>"
    case TpeRef(name, false, true, _)      => s"$name[]"
    case TpeRef(name, false, false, true)  => s"$optionalInterface<${boxedType(name)}>"
    case TpeRef(name, false, false, false) => name
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

  private def genMessages(messages: List[Message]) = messages map genMessage mkString EOL
  private def genMessage(message: Message) = {
    val requests = message.request map { case Request(name, _, tpe) => s"${genRealTpe(tpe)} $name" }
    val requestsDoc = message.request flatMap {
      case Request(name, Nil, _)        => Nil
      case Request(name, doc :: Nil, _) => s"@param $name $doc" :: Nil
      case Request(name, doc, _)        =>
        val prefix = s"@param $name "
        doc.mkString(prefix, EOL + " " * (prefix.length + 3), "") :: Nil
    }
    s"""${genDoc(message.doc ++ requestsDoc)}
       |public abstract ${genRealTpe(message.responseTpe)} ${message.name}(${requests mkString ","});"""
  }

  private def genConstructors(cl: ClassLike, parent: Option[Interface], superFields: List[Field]) =
    perVersionNumber(cl.since, superFields ++ cl.fields) { (provided, byDefault) =>
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

  private def genWith(r: Record, superFields: List[Field]) = {
    def capitalize(s: String) = { val (fst, rst) = s.splitAt(1) ; fst.toUpperCase + rst }
    val allFields = (superFields ++ r.fields).zipWithIndex
    def nonParam(f: (Field, Int)): String = {
      val field = f._1
      if (r.fields contains field) field.name
      else if (field.tpe.lzy) {
        val tpeSig =
          if (field.tpe.repeated) s"${field.tpe.name}[]"
          else field.tpe.name
        s"new ${genRealTpe(field.tpe)}() { public ${boxedType(tpeSig)} get() { return ${field.name}(); } }"
      } else s"${f._1.name}()"
    }

    allFields map { case (f, idx) =>
      val (before, after) = allFields filterNot (_._2 == idx) splitAt idx
      val params = (before map nonParam) ::: f.name :: (after map nonParam) mkString ", "
      s"""public ${r.name} with${capitalize(f.name)}(${genRealTpe(f.tpe)} ${f.name}) {
         |    return new ${r.name}($params);
         |}""".stripMargin
    } mkString (EOL + EOL)
  }

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
    val allFields = superFields ++ cl.fields
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
    val allFields = superFields ++ cl.fields
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
