package com.eed3si9n.datatype

import org.specs2._
import SchemaExample._

class CodeGenSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    CodeGen.generate should
      generate                                   $e1
  """

  def e1 = {
    val s = ProtocolSchema.parse(basicSchema)
    val code = CodeGen.generate(s)
    code must_== """package com.example

final class Greeting(message: String)"""
  }
}

