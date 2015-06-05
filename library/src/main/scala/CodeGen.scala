package sbt.datatype

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
      val fieldNames = td.fields map { field: FieldSchema => field.name }
      val fieldNamesCode = fieldNames.mkString(", ")
      val mainApply =
        s"""def apply($fieldsCode): $name =
    new $name($fieldNamesCode)"""
      s"""final class $name($fieldsCode) private {
  ${generateEquals(name, fieldNames)}
  ${generateHashCode(fieldNames)}
}

object $name {
  $mainApply
}"""
    }
  
  def generateEquals(name: String, fieldNames: Vector[String]): String =
    {
      val fieldNamesEq = fieldNames map { n: String => s"(this.$n == o.$n)" }
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

  def generateField(fs: FieldSchema): String =
    {
      val name = fs.name
      val tpe = fs.`type`.name
      s"""$name: $tpe"""
    }
}

