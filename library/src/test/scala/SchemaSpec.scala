package sbt.datatype

import org.specs2._
import SchemaExample._

class SchemaSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    ProtocolSchema.parse should
      parse                                      $e1
  """

  def e1 = {
    val s = ProtocolSchema.parse(basicSchema)
    s.namespace must_== "com.example"
  }
}

