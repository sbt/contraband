package sbt.datatype
import scala.compat.Platform.EOL
import java.io.File

/**
 * The base for code generators.
 */
abstract class CodeGenerator {

  /** When this predicate holds for `s`, this line and the following should have one more level of indentation. */
  protected def augmentIndentTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, next lines should have one more level of indentation. */
  protected def augmentIndentAfterTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, this line and the following should have one less level of indentation. */
  protected def reduceIndentTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, next lines should have one less level of indentation. */
  protected def reduceIndentAfterTrigger(s: String): Boolean = false

  /**
   * Implementation of a string buffer which takes care of indentation (according to `augmentIndentTrigger`,
   * `augmentIndentAfterTrigger`, `reduceIndentTrigger` and `reduceIndentAfterTrigger`) as text is added.
   */
  protected class IndentationAwareBuffer(val indent: String, private var level: Int = 0) {
    private val buffer: StringBuilder = new StringBuilder

    /** Add all the lines of `it` to the buffer. */
    def +=(it: Iterator[String]): Unit = it foreach append
    /** Add `s` to the buffer */
    def +=(s: String): Unit = s.lines foreach append

    override def toString: String = buffer.mkString

    private def append(s: String): Unit = {
      val clean = s.trim
      if (augmentIndentTrigger(clean)) level += 1
      if (reduceIndentTrigger(clean)) level = 0 max (level - 1)
      buffer append (indent * level + clean + EOL)
      if (augmentIndentAfterTrigger(clean)) level += 1
      if (reduceIndentAfterTrigger(clean)) level = 0 max (level - 1)
    }
  }

  /** Run an operation `op` with a new `IndentationAwareBuffer` and return its content. */
  protected def buffered(op: IndentationAwareBuffer => Unit): String

  /** Run an operation `op` for each different version number that affects the fields `fields`. */
  protected final def perVersionNumber[T](fields: List[Field])(op: (List[Field], List[Field]) => T): List[T] = {
    val versionNumbers = fields.map(_.since).sorted.distinct
    versionNumbers map { v =>
      val (provided, byDefault) = fields partition (_.since <= v)
      op(provided, byDefault)
    }
  }

  /** Generate the code corresponding to `d`. */
  protected final def generate(d: Definition, parent: Option[Protocol], superFields: List[Field]): Map[String, String] =
    d match {
      case p: Protocol    => generate(p, parent, superFields)
      case r: Record      => generate(r, parent, superFields)
      case e: Enumeration => generate(e)
    }

  /** Generate the code corresponding to all definitions in `s`. */
  def generate(s: Schema): Map[File, String]

  /** Generate the code corresponding to the protocol `p`. */
  protected def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[String, String]

  /** Generate the code corresponding to the record `r`. */
  protected def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[String, String]

  /** Generate the code corresponding to the enumeration `e`. */
  protected def generate(e: Enumeration): Map[String, String]

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

