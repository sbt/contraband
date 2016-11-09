package sbt.datatype

import scala.util.Try
import org.json4s._

/**
 * Offers functinos allowing to parse a representation of a
 * type `T` from JSON.
 */
trait Parser[T] {

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

  /** Parse an instance of `T` from `input`. */
  final def parse(input: String): T = {
    val json = jawn.support.json4s.Parser.parseFromString(input).get
    parse(json)
  }

  /** Parse an instance of `T` from `input`. */
  def parse(json: JValue): T

  val emptyVersion: VersionNumber = VersionNumber("0.0.0")

}

sealed trait SchemaElement {
  def name: String
  def doc: List[String]
}

/**
 * Base trait that represents `Interface`s, `Record`s and `Enumeration`s.
 * Syntax:
 *   Definition := Interface | Record | Enumeration
 */
sealed trait Definition extends SchemaElement {
  def namespace: Option[String]
  def targetLang: String
  def since: VersionNumber
  def extra: List[String]
}

sealed trait ClassLike extends Definition {
  def fields: List[Field]
}

object Definition extends Parser[Definition] {
  override def parse(json: JValue): Definition = {
    json -> "type" match {
      case "interface"            => Interface.parse(json)
      case "record"               => Record.parse(json)
      case "enum" | "enumeration" => Enumeration.parse(json)
      case other         => sys.error(s"Invalid type: $other")
    }
  }
}

/**
 * Represents a complete schema definition.
 * Syntax:
 *   Schema := { "types": [ Definition* ] }
 *             (, "codecNamespace": string constant)?
 *             (, "fullCodec": string constant)? }
 */
case class Schema(definitions: List[Definition],
  codecNamespace: Option[String],
  fullCodec: Option[String])

object Schema extends Parser[Schema] {
  override def parse(json: JValue): Schema =
    Schema(json ->* "types" map Definition.parse,
      json ->? "codecNamespace",
      json ->? "fullCodec")
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
case class Interface(name: String,
  targetLang: String,
  namespace: Option[String],
  since: VersionNumber,
  doc: List[String],
  fields: List[Field],
  messages: List[Message],
  children: List[Definition],
  extra: List[String],
  toStringBody: Option[String],
  extraCompanion: List[String],
  parents: List[String],
  parentsCompanion: List[String]) extends ClassLike

object Interface extends Parser[Interface] {
  override def parse(json: JValue): Interface =
    Interface(json -> "name",
      json -> "target",
      json ->? "namespace",
      json ->? "since" map VersionNumber.apply getOrElse emptyVersion,
      json multiLineOpt "doc" getOrElse Nil,
      json ->* "fields" map Field.parse,
      json ->* "messages" map Message.parse,
      json ->* "types" map Definition.parse,
      json multiLineOpt "extra" getOrElse Nil,
      json ->? "toString",
      json multiLineOpt "extraCompanion" getOrElse Nil,
      json multiLineOpt "parents" getOrElse Nil,
      json multiLineOpt "parentsCompanion" getOrElse Nil)
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
case class Record(name: String,
  targetLang: String,
  namespace: Option[String],
  since: VersionNumber,
  doc: List[String],
  fields: List[Field],
  extra: List[String],
  toStringImpl: Option[String],
  extraCompanion: List[String],
  parents: List[String],
  parentsCompanion: List[String]) extends ClassLike

object Record extends Parser[Record] {
  override def parse(json: JValue): Record =
    Record(json -> "name",
      json -> "target",
      json ->? "namespace",
      json ->? "since" map VersionNumber.apply getOrElse emptyVersion,
      json multiLineOpt "doc" getOrElse Nil,
      json ->* "fields" map Field.parse,
      json multiLineOpt "extra" getOrElse Nil,
      json ->? "toString",
      json multiLineOpt "extraCompanion" getOrElse Nil,
      json multiLineOpt "parents" getOrElse Nil,
      json multiLineOpt "parentsCompanion" getOrElse Nil)
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
case class Enumeration(name: String,
  targetLang: String,
  namespace: Option[String],
  since: VersionNumber,
  doc: List[String],
  values: List[EnumerationValue],
  extra: List[String]) extends Definition

object Enumeration extends Parser[Enumeration] {
  override def parse(json: JValue): Enumeration =
    Enumeration(json -> "name",
      json -> "target",
      json ->? "namespace",
      json ->? "since" map VersionNumber.apply getOrElse emptyVersion,
      json multiLineOpt "doc" getOrElse Nil,
      json ->* "symbols" map EnumerationValue.parse,
      json multiLineOpt "extra" getOrElse Nil)
}

/**
 * One of the values of an enumeration.
 * Syntax:
 *   EnumerationValue := ID
 *                     | {   "name": ID
                          (, "doc": string constant)? }
  */
case class EnumerationValue(name: String,
  doc: List[String]) extends SchemaElement

object EnumerationValue extends Parser[EnumerationValue] {
  override def parse(json: JValue): EnumerationValue =
    json match {
      case JString(name) => EnumerationValue(name, Nil)
      case json          => EnumerationValue(json -> "name", json multiLineOpt "doc" getOrElse Nil)
  }
}

/**
 * A field of an interface or record.
 * Syntax:
 *   Field := {   "name": ID,
 *                "type": ID
 *             (, "doc": string constant)?
 *             (, "since": version number string)?
 *             (, "default": string constant)? }
 */
case class Field(name: String,
  doc: List[String],
  tpe: TpeRef,
  since: VersionNumber,
  default: Option[String]) extends SchemaElement

object Field extends Parser[Field] {
  override def parse(json: JValue): Field =
    Field(json -> "name",
      json multiLineOpt "doc" getOrElse Nil,
      TpeRef(json -> "type"),
      json ->? "since" map VersionNumber.apply getOrElse emptyVersion,
      json ->? "default")
}

/**
 * An abstract method defined in an interface.
 * Syntax:
 *   Message := {   "name": ID,
 *                  "response": ID
 *               (, "request": [ Request* ])?
 *               (, "doc": string constant)? }
 */
case class Message(name: String,
  doc: List[String],
  responseTpe: TpeRef,
  request: List[Request]) extends SchemaElement

object Message extends Parser[Message] {
  override def parse(json: JValue): Message =
    Message(json -> "name",
      json multiLineOpt "doc" getOrElse Nil,
      TpeRef(json -> "response"),
      json ->* "request" map Request.parse)
}

/**
 * An argument of a method.
 * Syntax:
 *   Request := {   "name": ID,
 *                  "type": ID
 *               (, "doc": string constant)? }
 */
case class Request(name: String,
  doc: List[String],
  tpe: TpeRef) extends SchemaElement

object Request extends Parser[Request] {
  override def parse(json: JValue): Request =
    Request(json -> "name",
      json multiLineOpt "doc" getOrElse Nil,
      TpeRef(json -> "type"))
}

case class TpeRef(name: String, lzy: Boolean, repeated: Boolean, optional: Boolean)

object TpeRef {
  import scala.util.matching.Regex
  private val LazyRepeated = """^lazy (.+?)\*$""".r
  private val LazyOptional = """^lazy (.+?)\?$""".r
  private val Lazy = """^lazy (.+?)$""".r
  private val Repeated = """^(.+?)\*$""".r
  private val Optional = """^(.+?)\?$""".r

  def apply(str: String): TpeRef = str match {
    case LazyRepeated(tpe) => TpeRef(tpe, true, true, false)
    case LazyOptional(tpe) => TpeRef(tpe, true, false, true)
    case Lazy(tpe)         => TpeRef(tpe, true, false, false)
    case Repeated(tpe)     => TpeRef(tpe, false, true, false)
    case Optional(tpe)     => TpeRef(tpe, false, false, true)
    case tpe               => TpeRef(tpe, false, false, false)
  }
}



case class ProtocolSchema(namespace: String,
  protocol: String,
  doc: String,
  types: Vector[TypeDef])

object ProtocolSchema {
  def parse(input: String): ProtocolSchema =
    {
      val json = jawn.support.json4s.Parser.parseFromString(input).get
      parse(json)
    }
  def parse(json: JValue): ProtocolSchema =
    {
      val xs = for {
        JObject(ps) <- json
        JField("namespace", JString(namespace)) <- ps
        JField("protocol", JString(protocol)) <- ps
        JField("doc", JString(doc)) <- ps
        JField("types", JArray(types)) <- ps
      } yield ProtocolSchema(namespace, protocol, doc,
        types.toVector map { TypeDef.parse })
      xs.headOption getOrElse sys.error(s"Invalid schema: $json")
    }
}

sealed trait Type {
  def name: String
}

object Type {
  def parse(json: JValue): Type =
    json match {
      case JString(str) => TypeRef.lookupType(str)
      case JObject(obj) => TypeDef.parse(JObject(obj))
      case _ => sys.error(s"Unsupported type: $json")
    }
}

case class TypeRef(name: String) extends Type

object TypeRef {
  def lookupType(name: String): TypeRef =
    name match {
      case "string"  => TypeRef("String")
      case "boolean" => TypeRef("Boolean")
      case "int"     => TypeRef("Int")
      case "long"    => TypeRef("Long")
      case "float"   => TypeRef("Float")
      case "double"  => TypeRef("Double")
      case x         => TypeRef(x)
    }
}


case class TypeDef(name: String,
  `type`: String,
  fields: Vector[FieldSchema]) extends Type

object TypeDef {
  def parse(json: JValue): TypeDef =
    {
      val xs = for {
        JObject(ts) <- json
        JField("name", JString(name)) <- ts
        JField("type", JString(tpe)) <- ts
        JField("fields", JArray(fields)) <- ts
      } yield TypeDef(name, tpe,
        fields.toVector map { FieldSchema.parse })
      xs.headOption getOrElse sys.error(s"Invalid schema: $json")
    }
}

case class FieldSchema(name: String,
  `type`: Type,
  since: VersionNumber,
  defaultValue: Option[String])

object FieldSchema {
  val emptyVersion: VersionNumber = VersionNumber("0.0.0")
  def parse(json: JValue): FieldSchema =
    {
      val xs = for {
        JObject(ts) <- json
        JField("name", JString(name)) <- ts
        JField("type", tpe) <- ts
      } yield FieldSchema(name,
        Type.parse(tpe),
        (json \ "since").toOption map {
          case JString(str) => VersionNumber(str)
          case json         => sys.error(s"Invalid since: $json")
        } getOrElse emptyVersion,
        (json \ "default").toOption map {
          case JString(str) => str
          case json         => sys.error(s"Invalid since: $json")
        }
      )
      xs.headOption getOrElse sys.error(s"Invalid schema: $json")
    }
}
