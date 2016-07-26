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
      parse interface                            $definitionParseInterface
      parse record                               $definitionParseRecord
      parse enumeration                          $definitionParseEnumeration
      throw an error on invalid definition kind  $definitionParseInvalidDefinitionKind

    Interface.parse should
      parse simple interface                     $interfaceParseSimple
      parse interface with one child             $interfaceParseOneChild
      parse nested interfaces                    $interfaceParseNested

    Record.parse should
      parse simple record                        $recordParseSimple

    Enumeration.parse should
      parse simple enumeration                   $enumerationParseSimple

    Message.parse should
      parse                                      $messageParse

    Field.parse should
      parse                                      $fieldParse
      parse multiline doc comments               $multiLineDoc

    TpeRef.apply should
      parse simple types                         $tpeRefParseSimple
      parse lazy types                           $tpeRefParseLazy
      parse array types                          $tpeRefParseArray
      parse option types                         $tpeRefParseOption
      parse lazy array types                     $tpeRefParseLazyArray
      parse lazy option types                    $tpeRefParseLazyOption
  """

  def e1 = {
    val s = ProtocolSchema.parse(basicSchema)
    s.namespace must_== "com.example"
  }

  def emptySchema = {
    val s = Schema parse emptySchemaExample
    s.definitions must_== Nil
  }

  def complete = {
    val s = Schema parse completeExample
    s.definitions must haveSize(3)
  }

  def definitionParseInterface = {
    Definition parse emptyInterfaceExample match {
      case Interface(name, target, namespace, _, doc, fields, abstractMethods, children, extra) =>
        (name must_== "emptyInterfaceExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List()) and
        (fields must haveSize(0)) and
        (abstractMethods must haveSize(0)) and
        (children must haveSize(0)) and
        (extra must_== List())

      case _ =>
        true must_== false
    }
  }

  def definitionParseRecord = {
    Definition parse emptyRecordExample match {
      case Record(name, target, namespace, _, doc, fields, extra) =>
        (name must_== "emptyRecordExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List()) and
        (fields must haveSize(0)) and
        (extra must_== List())

      case _ =>
        true must_== false
    }
  }

  def definitionParseEnumeration = {
    Definition parse emptyEnumerationExample match {
      case Enumeration(name, target, namespace, _, doc, values, extra) =>
        (name must_== "emptyEnumerationExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List()) and
        (values must haveSize(0))
        (extra must_== List())

      case _ =>
        true must_== false
    }
  }

  def definitionParseInvalidDefinitionKind =
    Definition parse invalidDefinitionKindExample must throwA[RuntimeException]

  def interfaceParseSimple = {
    Interface parse simpleInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        (name must_== "simpleInterfaceExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List("example of simple interface")) and
        (fields must haveSize(1)) and
        (fields(0) must_== Field("field", Nil, TpeRef("type", false, false, false), Field.emptyVersion, None)) and
        (abstractMethods must haveSize(0)) and
        (children must haveSize(0)) and
        (extra must_== List("// Some extra code..."))
    }
  }

  def interfaceParseOneChild = {
    Interface parse oneChildInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        (name must_== "oneChildInterfaceExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List("example of interface")) and
        (fields must haveSize(1)) and
        (fields(0) must_== Field("field", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None)) and
        (abstractMethods must haveSize(0)) and
        (children must haveSize(1)) and
        (children(0) must_== Record("childRecord", "Scala", None, VersionNumber("0.0.0"), Nil,
          Field("x", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None) :: Nil, Nil)) and
        (extra must_== List())
    }
  }

  def interfaceParseNested = {
    Interface parse nestedInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        (name must_== "nestedProtocolExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List("example of nested protocols")) and
        (fields must haveSize(0)) and
        (abstractMethods must haveSize(0)) and
        (children must haveSize(1)) and
        (children(0) must_== Interface("nestedProtocol", "Scala", None, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil, Nil)) and
        (extra must_== List())
    }
  }

  def recordParseSimple = {
    Record parse simpleRecordExample match {
      case Record(name, target, namespace, since, doc, fields, extra) =>
        (name must_== "simpleRecordExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List("Example of simple record")) and
        (fields must haveSize(1)) and
        (fields(0) must_== Field("field", Nil, TpeRef("java.net.URL", false, false, false), Field.emptyVersion, None)) and
        (extra must_== List("// Some extra code..."))
    }
  }

  def enumerationParseSimple = {
    Enumeration parse simpleEnumerationExample match {
      case Enumeration(name, target, namespace, since, doc, values, extra) =>
        (name must_== "simpleEnumerationExample") and
        (target must_== "Scala") and
        (namespace must_== None) and
        (doc must_== List("Example of simple enumeration")) and
        (values must haveSize(2)) and
        (values(0) must_== EnumerationValue("first", List("First symbol"))) and
        (values(1) must_== EnumerationValue("second", Nil)) and
        (extra must_== List("// Some extra code..."))
    }
  }

  def fieldParse = {
    Field parse fieldExample match {
      case Field(name, doc, tpe, since, default) =>
        (name must_== "fieldExample") and
        (doc must_== List("Example of field")) and
        (tpe must_== TpeRef("type", false, false, false)) and
        (since must_== VersionNumber("1.0.0")) and
        (default must_== Some("2 + 2"))
    }

  }

  def messageParse = {
    Message parse messageExample match {
      case Message(name, doc, retTpe, request) =>
        (name must_== "messageExample") and
        (doc must_== List("Example of a message")) and
        (retTpe must_== TpeRef("int", false, false, false)) and
        (request must_== List(Request("arg0", Nil, TpeRef("type2", false, false, false))))
    }
  }

  def tpeRefParseSimple = {
    TpeRef apply simpleTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "simpleTpeRefExample") and
        (lzy must_== false) and
        (repeated must_== false) and
        (opt must_== false)
    }
  }

  def tpeRefParseLazy = {
    TpeRef apply lazyTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "lazyTpeRefExample") and
        (lzy must_== true) and
        (repeated must_== false) and
        (opt must_== false)
    }
  }

  def tpeRefParseArray = {
    TpeRef apply arrayTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "arrayTpeRefExample") and
        (lzy must_== false) and
        (repeated must_== true) and
        (opt must_== false)
    }
  }

  def tpeRefParseOption = {
    TpeRef apply optionTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "optionTpeRefExample") and
        (lzy must_== false) and
        (repeated must_== false) and
        (opt must_== true)
    }
  }

  def tpeRefParseLazyArray = {
    TpeRef apply lazyArrayTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "lazyArrayTpeRefExample") and
        (lzy must_== true) and
        (repeated must_== true) and
        (opt must_== false)
    }
  }

  def tpeRefParseLazyOption = {
    TpeRef apply lazyOptionTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        (name must_== "lazyOptionTpeRefExample") and
        (lzy must_== true) and
        (repeated must_== false) and
        (opt must_== true)
    }
  }

  def multiLineDoc = {
    Field parse multiLineDocExample match {
      case Field(name, doc, tpe, since, default) =>
        (name must_== "multiLineDocField") and
        (doc must_== List("A field whose documentation",
                              "spans over multiple lines"))

      case _ =>
        true must_== false
    }
  }

}
