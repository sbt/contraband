package com.eed3si9n.datatype

object CodeGen {
  def generate(ps: ProtocolSchema): String =
    {
      val ns = ps.namespace
      val types = ps.types map { tpe: TypeDef => generateType(tpe) }
      val typesCode = types.mkString("\n")
      s"""package $ns

$typesCode"""
    }

  def generateType(td: TypeDef): String =
    {
      val name = td.name
      val fields = td.fields map { field: FieldSchema => generateField(field) }
      val fieldsCode = fields.mkString(",\n  ")
      s"""final class $name($fieldsCode)"""
    }

  def generateField(fs: FieldSchema): String =
    {
      val name = fs.name
      val tpe = fs.`type`.name
      s"""$name: $tpe"""
    }
}

