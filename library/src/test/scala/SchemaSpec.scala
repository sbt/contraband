package sbt.datatype

import org.specs2._
import SchemaExample._
import NewSchema._

class SchemaSpec extends Specification {
  def is = s2"""
    This is a specification for data type spec.

    ProtocolSchema.parse should
      parse                                      $e1

    Schema.parse should
      parse empty Schemas                        $emptySchema
      parse complete example                     $complete

    Definition.parse should
      parse protocol                             $definitionParseProtocol
      parse record                               $definitionParseRecord
      parse enumeration                          $definitionParseEnumeration
      throw an error on invalid definition kind  $definitionParseInvalidDefinitionKind

    Protocol.parse should
      parse simple protocol                      $protocolParseSimple
      parse protocol with one child              $protocolParseOneChild
      parse nested protocols                     $protocolParseNested
  """

  def e1 = {
    val s = ProtocolSchema.parse(basicSchema)
    s.namespace must_== "com.example"
  }

  def emptySchema = {
    val s = Schema parse emptySchemaExample
    s.namespace must_== "com.example"
    s.definitions must_== Nil
  }

  def complete = {
    val s = Schema parse completeExample
    s.namespace must_== "com.example"
    s.definitions must haveSize(3)
  }

  def definitionParseProtocol = {
    Definition parse emptyProtocolExample match {
      case Protocol(name, doc, fields, children) =>
        name must_== "emptyProtocolExample"
        doc must_== None
        fields must haveSize(0)
        children must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseRecord = {
    Definition parse emptyRecordExample match {
      case Record(name, doc, fields) =>
        name must_== "emptyRecordExample"
        doc must_== None
        fields must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseEnumeration = {
    Definition parse emptyEnumerationExample match {
      case Enumeration(name, doc, values) =>
        name must_== "emptyEnumerationExample"
        doc must_== None
        values must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseInvalidDefinitionKind =
    Definition parse invalidDefinitionKindExample must throwA[RuntimeException]

  def protocolParseSimple = {
    Protocol parse simpleProtocolExample match {
      case Protocol(name, doc, fields, children) =>
        name must_== "simpleProtocolExample"
        doc must_== Some("example of simple protocol")
        fields must haveSize(1)
        fields(0) must_== Field("field", None, TpeRef("type", false, false), Field.emptyVersion, None)
        children must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def protocolParseOneChild = {
    Protocol parse oneChildProtocolExample match {
      case Protocol(name, doc, fields, children) =>
        name must_== "oneChildProtocolExample"
        doc must_== Some("example of protocol")
        fields must haveSize(0)
        children must haveSize(1)
        children(0) must_== Record("childRecord", None, Nil)
    }
  }

  def protocolParseNested = {
    Protocol parse nestedProtocolExample match {
      case Protocol(name, doc, fields, children) =>
        name must_== "nestedProtocolExample"
        doc must_== Some("example of nested protocols")
        fields must haveSize(0)
        children must haveSize(1)
        children(0) must_== Protocol("nestedProtocol", None, Nil, Nil)
    }
  }

}
