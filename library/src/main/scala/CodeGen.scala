package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File
import scala.collection.immutable.ListMap

/**
 * The base for code generators.
 */
abstract class CodeGenerator {

  implicit class ListMapOp[T](m: ListMap[T, String]) {
    def merge(o: ListMap[T, String]): ListMap[T, String] =
      (o foldLeft m) { case (acc, (k, v)) =>
        val existing = acc get k getOrElse ""

        acc get k match {
          case None =>
            acc + (k -> v)

          case Some(existing) =>
            // Remove `package blah` from what we want to add
            val content = v.lines.toList.tail mkString EOL
            acc + (k -> (existing + EOL + EOL + content))
        }
      }

    def mapV(f: String => String): ListMap[T, String] =
      ListMap(m.toList map { case (k, v) =>
        (k, f(v))
      }: _*)
  }

  implicit protected class IndentationAwareString(code: String) {
    final def indented(implicit config: IndentationConfiguration): String = indentWith(config)

    final def indentWith(config: IndentationConfiguration): String = {
      val buffer = new IndentationAwareBuffer(config)
      code.lines foreach buffer .+=
      buffer.toString
    }
  }


  /** Run an operation `op` for each different version number that affects the fields `fields`. */
  protected final def perVersionNumber[T](since: VersionNumber, fields: List[Field])(op: (List[Field], List[Field]) => T): List[T] = {
    val versionNumbers = (since :: fields.map(_.since)).sorted.distinct
    versionNumbers map { v =>
      val (provided, byDefault) = fields partition (_.since <= v)
      op(provided, byDefault)
    }
  }

  /** Generate the code corresponding to all definitions in `s`. */
  def generate(s: Schema): ListMap[File, String]

  /** Generate the code corresponding to `d`. */
  protected final def generate(s: Schema, d: Definition, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] =
    d match {
      case i: Interface   => generateInterface(s, i, parent, superFields)
      case r: Record      => generateRecord(s, r, parent, superFields)
      case e: Enumeration => generateEnum(s, e)
    }

  /** Generate the code corresponding to the interface `i`. */
  protected def generateInterface(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String]

  /** Generate the code corresponding to the record `r`. */
  protected def generateRecord(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): ListMap[File, String]

  /** Generate the code corresponding to the enumeration `e`. */
  protected def generateEnum(s: Schema, e: Enumeration): ListMap[File, String]

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

