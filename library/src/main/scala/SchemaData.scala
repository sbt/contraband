package com.eed3si9n.datatype

import scala.util.Try
import org.json4s._

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

sealed trait Type

object Type {
  def parse(json: JValue): Type =
    json match {
      case JString(str) => TypeRef(str)
      case JObject(obj) => TypeDef.parse(JObject(obj))
      case _ => sys.error(s"Unsupported type: $json")
    }
}

case class TypeRef(name: String) extends Type

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
  `type`: Type)

object FieldSchema {
  def parse(json: JValue): FieldSchema =
    {
      val xs = for {
        JObject(ts) <- json
        JField("name", JString(name)) <- ts
        JField("type", tpe) <- ts
      } yield FieldSchema(name,
        Type.parse(tpe))
      xs.headOption getOrElse sys.error(s"Invalid schema: $json")
    }
}

