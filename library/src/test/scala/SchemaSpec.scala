package com.eed3si9n.datatype

import org.specs2._

class SchemaSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    ProtocolSchema.parse should
      parse                                      $e1
  """

  def e1 = {
    val s = ProtocolSchema.parse(exampleJson)
    s.namespace must_== "com.example"
  }

  val exampleJson = """{
  "namespace": "com.example",
  "protocol": "HelloWorld",
  "doc": "Protocol Greetings",

  "types": [
    {"name": "Greeting", "type": "record", "fields": [
      {"name": "message", "type": "string"}]}
  ]
}"""

}

