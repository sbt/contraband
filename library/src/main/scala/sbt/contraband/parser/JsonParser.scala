package sbt.contraband
package parser


import scala.util.Try
import org.json4s._
import ast.AstUtil.toNamedType

/**
 * Offers functinos allowing to parse a representation of a
 * type `T` from JSON.
 */
trait JsonParser[T] {

  implicit class JSONHelper(jValue: JValue) {
    /** Optionally retrieves the string field `key` from `jValue`. */
    def ->?(key: String): Option[String] = (jValue \ key).toOption map {
      case JString(value) => value
      case json           => sys.error(s"Invalid $key: $json")
    }

    /** Retrieves the string field `key` from `jValue`. */
    def ->(key: String): String = this ->? key getOrElse sys.error(s"""Undefined key "$key": $jValue""")

    /** Optionally retrieves the array field `key` from `jValue`. */
    def ->*?(key: String): Option[List[JValue]] = (jValue \ key).toOption map {
      case JArray(values) => values.toList
      case json           => sys.error(s"Invalid $key: $json")
    }

    /** Retrieves the array field `key` from `jValue`. */
    def ->*(key: String): List[JValue] = this ->*? key getOrElse Nil

    /** Optionally retrieves an array of strings or a single string */
    def multiLineOpt(key: String): Option[List[String]] = (jValue \ key).toOption map {
      case JString(value) => List(value)
      case JArray(values) => values collect { case JString(value) => value }
      case other          => sys.error(s"Expected string or array, found $other.")
    }

    /** Retrieves an array and concatenates its string values in multiple lines, or retrieves a string. */
    def multiLine(key: String): List[String] = multiLineOpt(key) getOrElse sys.error(s"""Undefined "$key" or wrong type: $jValue""")
  }

  def DocComment(json: JValue): List[ast.DocComment] =
    (json multiLineOpt "doc") match {
      case Some(xs) => xs map { ast.DocComment(_) }
      case _        => Nil
    }

  def ExtraComment(json: JValue): List[ast.ExtraComment] =
    (json multiLineOpt "extra") match {
      case Some(xs) => xs map { ast.ExtraComment(_) }
      case _        => Nil
    }

  def ExtraIntfComment(json: JValue): List[ast.ExtraIntfComment] =
    (json multiLineOpt "parents") match {
      case Some(xs) => xs map { ast.ExtraIntfComment(_) }
      case _        => Nil
    }

  def ToStringImplComment(json: JValue): List[ast.ToStringImplComment] =
    (json multiLineOpt "toString") match {
      case Some(xs) => xs map { ast.ToStringImplComment(_) }
      case _        => Nil
    }

  def CompanionExtraIntfComment(json: JValue): List[ast.CompanionExtraIntfComment] =
    (json multiLineOpt "parentsCompanion") match {
      case Some(xs) => xs map { ast.CompanionExtraIntfComment(_) }
      case _        => Nil
    }

  def CompanionExtraComment(json: JValue): List[ast.CompanionExtraComment] =
    (json multiLineOpt "extraCompanion") match {
      case Some(xs) => xs map { ast.CompanionExtraComment(_) }
      case _        => Nil
    }

  def TargetDirective(json: JValue): Option[ast.Directive] =
    (json ->? "target") map {
      case "Java"  => ast.Directive.targetJava
      case "Scala" => ast.Directive.targetScala
    }

  def SinceDirective(json: JValue): Option[ast.Directive] =
    (json ->? "since") match {
      case Some(x) => Some(ast.Directive.since(x))
      case _       => Some(ast.Directive.since(VersionNumber.empty.toString))
    }

  def ModifierDirective(json: JValue): Option[ast.Directive] =
    (json ->? "modifier") match {
      case Some(x) => Some(ast.Directive.modifier(x))
      case _       => None
    }

  def CodecPackageDirective(json: JValue): Option[ast.Directive] =
    (json ->? "codecNamespace") orElse (json ->? "codecPackage") match {
      case Some(x) => Some(ast.Directive.codecPackage(x))
      case _       => None
    }

  def FullCodecDirective(json: JValue): Option[ast.Directive] =
    (json ->? "fullCodec") match {
      case Some(x) => Some(ast.Directive.fullCodec(x))
      case _       => None
    }

  def GenerateCodecDirective(json: JValue): Option[ast.Directive] =
    (json \ "generateCodec").toOption map {
      case JBool(value) => ast.Directive.generateCodec(value)
      case json         => sys.error(s"Invalid generateCodec: $json")
    }

  val emptyVersion: VersionNumber = VersionNumber("0.0.0")
}

trait Parse[T] extends JsonParser[T] {
  /** Parse an instance of `T` from `input`. */
  final def parse(input: String): T = {
    val json = Compat.Parser.parseFromString(input).get
    parse(json)
  }

  /** Parse an instance of `T` from `input`. */
  def parse(json: JValue): T
}

trait ParseWithSuperIntf[A] extends JsonParser[A] {
  final def parse(input: String): A =
    parse(input, None)
  final def parse(input: String, superIntf: Option[ast.InterfaceTypeDefinition]): A = {
    val json = Compat.Parser.parseFromString(input).get
    parse(json, superIntf)
  }
  final def parse(json: JValue): A =
    parse(json, None)
  def parse(json: JValue, superIntf: Option[ast.InterfaceTypeDefinition]): A
}

object JsonParser {
  /**
   * Represents a complete schema definition.
   * Syntax:
   *   Schema := { "types": [ Definition* ] }
   *             (, "codecNamespace": string constant)?
   *             (, "fullCodec": string constant)? }
   */
  object Document extends Parse[ast.Document] {
    override def parse(json: JValue): ast.Document =
      {
        val types = json ->* "types" flatMap parser.JsonParser.TypeDefinitions.parse
        val directives = CodecPackageDirective(json).toList ++ FullCodecDirective(json).toList
        ast.Document(
          None,
          types,
          directives,
          Nil,
          None
        )
      }
  }

  object TypeDefinitions extends ParseWithSuperIntf[List[ast.TypeDefinition]] {
    override def parse(json: JValue, superIntf: Option[ast.InterfaceTypeDefinition]): List[ast.TypeDefinition] = {
      json -> "type" match {
        case "interface"            => InterfaceTypeDefinition.parseInterface(json, superIntf)
        case "record"               => ObjectTypeDefinition.parse(json, superIntf) :: Nil
        case "enum" | "enumeration" => EnumTypeDefinition.parse(json) :: Nil
        case other                  => sys.error(s"Invalid type: $other")
      }
    }
  }

  /**
   * Definition of an Enumeration.
   * Syntax:
   *   Enumeration := {   "name": ID
   *                      "target": ("Scala" | "Java" | "Mixed")
   *                   (, "namespace": string constant)?
   *                   (, "doc": string constant)?
   *                   (, "symbols": [ EnumerationValue* ])?
   *                   (, "extra": string constant)? }
   */
  object EnumTypeDefinition extends Parse[ast.EnumTypeDefinition] {
    override def parse(json: JValue): ast.EnumTypeDefinition =
      ast.EnumTypeDefinition(json -> "name",
        json ->? "namespace",
        (json ->* "symbols") map EnumValueDefinition.parse,
        TargetDirective(json).toList ++ GenerateCodecDirective(json).toList,
        // json ->? "since" map VersionNumber.apply getOrElse emptyVersion,
        DocComment(json) ++ ExtraComment(json))
  }

  /**
   * One of the values of an enumeration.
   * Syntax:
   *   EnumerationValue := ID
   *                     | {   "name": ID
                            (, "doc": string constant)? }
    */
  object EnumValueDefinition extends Parse[ast.EnumValueDefinition] {
    override def parse(json: JValue): ast.EnumValueDefinition =
      json match {
        case JString(name) => ast.EnumValueDefinition(name, Nil)
        case json          => ast.EnumValueDefinition(json -> "name", Nil, DocComment(json))
    }
  }

  /**
   * Records map to concrete classes.
   * Syntax:
   *   Record := {   "name": ID
   *                 "target": ("Scala" | "Java" | "Mixed")
   *              (, "namespace": string constant)?
   *              (, "doc": string constant)?
   *              (, "fields": [ Field* ])?
   *              (, "extra": string constant)? }
   */
  object ObjectTypeDefinition extends ParseWithSuperIntf[ast.ObjectTypeDefinition] {
    def parse(json: JValue, superIntf: Option[ast.InterfaceTypeDefinition]): ast.ObjectTypeDefinition =
      {
        val superFields =
          superIntf match {
            case Some(s) => s.fields
            case _       => Nil
          }
        val fs = json ->* "fields" map FieldDefinition.parse
        val intfs = (superIntf map { i => toNamedType(i, None) }).toList
        val directives = TargetDirective(json).toList ++
          SinceDirective(json).toList ++
          ModifierDirective(json).toList ++
          GenerateCodecDirective(json).toList
        ast.ObjectTypeDefinition(json -> "name",
          json ->? "namespace",
          intfs,
          superFields ++ fs,
          directives,
          DocComment(json),
          ExtraComment(json) ++ ExtraIntfComment(json) ++ ToStringImplComment(json) ++
          CompanionExtraIntfComment(json) ++ CompanionExtraComment(json),
          None)
      }
  }

  /**
   * Interface maps to an abstract classes.
   * Syntax:
   *   Interface := {  "name": ID,
   *                   "target": ("Scala" | "Java" | "Mixed")
   *                (, "namespace": string constant)?
   *                (, "doc": string constant)?
   *                (, "fields": [ Field* ])?
   *                (, "messages": [ Message* ])?
   *                (, "types": [ Definition* ])?
   *                (, "extra": string constant)? }
   */
  object InterfaceTypeDefinition extends JsonParser[ast.InterfaceTypeDefinition] {
    def parseInterface(input: String): List[ast.TypeDefinition] =
      parseInterface(input, None)
    def parseInterface(input: String, superIntf: Option[ast.InterfaceTypeDefinition]): List[ast.TypeDefinition] = {
      val json = Compat.Parser.parseFromString(input).get
      parseInterface(json, superIntf)
    }
    def parseInterface(json: JValue): List[ast.TypeDefinition] =
      parseInterface(json, None)
    def parseInterface(json: JValue, superIntf: Option[ast.InterfaceTypeDefinition]): List[ast.TypeDefinition] =
      {
        val superFields =
          superIntf match {
            case Some(s) => s.fields
            case _       => Nil
          }
        val fs = (json ->* "fields" map FieldDefinition.parse) ++
          (json ->* "messages" map FieldDefinition.parseMessage)
        val parents = json multiLineOpt "parents" getOrElse Nil
        val intfs = (superIntf map { i => toNamedType(i, None) }).toList
        val directives = TargetDirective(json).toList ++ SinceDirective(json).toList ++
          GenerateCodecDirective(json).toList
        val intf = ast.InterfaceTypeDefinition(json -> "name",
          json ->? "namespace",
          intfs,
          superFields ++ fs,
          directives,
          DocComment(json),
          ExtraComment(json) ++ ExtraIntfComment(json) ++ ToStringImplComment(json) ++
          CompanionExtraIntfComment(json) ++ CompanionExtraComment(json),
          None) // position
        val childTypes = (json ->* "types") flatMap { j => TypeDefinitions.parse(j, Some(intf)) }
        intf :: childTypes
      }
  }

  object FieldDefinition extends Parse[ast.FieldDefinition] {
    override def parse(json: JValue): ast.FieldDefinition =
      {
        val arguments = Nil
        val defaultValue = (json ->? "default") map { ast.RawValue(_) }
        val directives = SinceDirective(json).toList
        val tpe = Type.parse(json -> "type")
        ast.FieldDefinition(json -> "name",
          tpe,
          arguments,
          defaultValue,
          directives,
          DocComment(json),
          None)
      }

    def parseMessage(input: String): ast.FieldDefinition = {
      val json = Compat.Parser.parseFromString(input).get
      parseMessage(json)
    }
    def parseMessage(json: JValue): ast.FieldDefinition =
      {
        val arguments = (json ->* "request") map InputValueDefinition.parse
        val defaultValue = (json ->? "default") map { ast.RawValue(_) }
        val directives = SinceDirective(json).toList
        val tpe = Type.parse(json -> "response")
        ast.FieldDefinition(json -> "name",
          tpe,
          arguments,
          defaultValue,
          directives,
          DocComment(json),
          None)
      }
  }

  object InputValueDefinition extends Parse[ast.InputValueDefinition] {
    override def parse(json: JValue): ast.InputValueDefinition =
      {
        val arguments = Nil
        val defaultValue = (json ->? "default") map { ast.RawValue(_) }
        val directives = SinceDirective(json).toList
        val tpe = Type.parse(json -> "type")
        ast.InputValueDefinition(json -> "name",
          tpe,
          defaultValue,
          directives,
          DocComment(json),
          None)
      }
  }

  object Type {
    private case class X(name: String, lzy: Boolean, repeated: Boolean, optional: Boolean)
    private object X {
      import scala.util.matching.Regex
      private val LazyRepeated = """^lazy (.+?)\*$""".r
      private val LazyOptional = """^lazy (.+?)\?$""".r
      private val Lazy = """^lazy (.+?)$""".r
      private val Repeated = """^(.+?)\*$""".r
      private val Optional = """^(.+?)\?$""".r

      def apply(str: String): X = str match {
        case LazyRepeated(tpe) => X(tpe, true, true, false)
        case LazyOptional(tpe) => X(tpe, true, false, true)
        case Lazy(tpe)         => X(tpe, true, false, false)
        case Repeated(tpe)     => X(tpe, false, true, false)
        case Optional(tpe)     => X(tpe, false, false, true)
        case tpe               => X(tpe, false, false, false)
      }
    }

    def parse(s: String): ast.Type =
      {
        val r = X(s)
        val t0 = ast.NamedType(r.name, None)
        val t1 = if (r.repeated) ast.ListType(t0, None)
                 else if (!r.optional) ast.NotNullType(t0, None)
                 else t0
        val t2 = if (r.lzy) ast.LazyType(t1)
                 else t1
        t2
      }
  }
}
