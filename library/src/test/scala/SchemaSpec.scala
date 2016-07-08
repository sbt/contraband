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
      parse simple interface                     $protocolParseSimple
      parse interface with one child             $protocolParseOneChild
      parse nested interfaces                    $protocolParseNested

    Record.parse should
      parse simple record                        $recordParseSimple

    Enumeration.parse should
      parse simple enumeration                   $enumerationParseSimple

    Field.parse should
      parse                                      $fieldParse
      parse multiline doc comments               $multiLineDoc

    TpeRef.apply should
      parse simple types                         $tpeRefParseSimple
      parse lazy types                           $tpeRefParseLazy
      parse array types                          $tpeRefParseArray
      parse lazy array types                     $tpeRefParseLazyArray
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
      case Interface(name, target, namespace, _, doc, fields, abstractMethods, children) =>
        name must_== "emptyInterfaceExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== None
        fields must haveSize(0)
        abstractMethods must haveSize(0)
        children must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseRecord = {
    Definition parse emptyRecordExample match {
      case Record(name, target, namespace, _, doc, fields) =>
        name must_== "emptyRecordExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== None
        fields must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseEnumeration = {
    Definition parse emptyEnumerationExample match {
      case Enumeration(name, target, namespace, _, doc, values) =>
        name must_== "emptyEnumerationExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== None
        values must haveSize(0)

      case _ =>
        true must_== false
    }
  }

  def definitionParseInvalidDefinitionKind =
    Definition parse invalidDefinitionKindExample must throwA[RuntimeException]

  def protocolParseSimple = {
    Interface parse simpleProtocolExample match {
      case Interface(name, target, namespace, doc, _, fields, abstractMethods, children) =>
        name must_== "simpleProtocolExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== Some("example of simple interface")
        fields must haveSize(1)
        fields(0) must_== Field("field", Nil, TpeRef("type", false, false), Field.emptyVersion, None)
        abstractMethods must haveSize(0)
        children must haveSize(0)
    }
  }

  def protocolParseOneChild = {
    Interface parse oneChildProtocolExample match {
      case Interface(name, target, namespace, doc, _, fields, abstractMethods, children) =>
        name must_== "oneChildProtocolExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== Some("example of interface")
        fields must haveSize(0)
        abstractMethods must haveSize(0)
        children must haveSize(1)
        children(0) must_== Record("childRecord", "Scala", None, VersionNumber("0.0.0"), Nil, Nil)
    }
  }

  def protocolParseNested = {
    Interface parse nestedProtocolExample match {
      case Interface(name, target, namespace, doc, _, fields, abstractMethods, children) =>
        name must_== "nestedProtocolExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== Some("example of nested protocols")
        fields must haveSize(0)
        abstractMethods must haveSize(0)
        children must haveSize(1)
        children(0) must_== Interface("nestedProtocol", "Scala", None, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil)
    }
  }

  def recordParseSimple = {
    Record parse simpleRecordExample match {
      case Record(name, target, namespace, _, doc, fields) =>
        name must_== "simpleRecordExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== Some("Example of simple record")
        fields must haveSize(1)
        fields(0) must_== Field("field", Nil, TpeRef("java.net.URL", false, false), Field.emptyVersion, None)
    }
  }

  def enumerationParseSimple = {
    Enumeration parse simpleEnumerationExample match {
      case Enumeration(name, target, namespace, _, doc, values) =>
        name must_== "simpleEnumerationExample"
        target must_== "Scala"
        namespace must_== None
        doc must_== Some("Example of simple enumeration")
        values must haveSize(2)
        values(0) must_== EnumerationValue("first", List("First type"))
        values(1) must_== EnumerationValue("second", Nil)
    }
  }

  def fieldParse = {
    Field parse fieldExample match {
      case Field(name, doc, tpe, since, default) =>
        name must_== "fieldExample"
        doc must_== "Example of field"
        tpe must_== TpeRef("type", false, false)
        since must_== VersionNumber("1.0.0")
        default must_== Some("2 + 2")
    }

  }

  def abstractMethodParse = {
    AbstractMethod parse abstractMethodExample match {
      case AbstractMethod(name, doc, retTpe, args) =>
        name must_== "abstractMethodExample"
        doc must_== "Example of abstract method"
        retTpe must_== TpeRef("type", false, false)
        args must_== List(Arg("arg0", Nil, TpeRef("type2", false, false)))
    }
  }

  def tpeRefParseSimple = {
    TpeRef apply simpleTpeRefExample match {
      case TpeRef(name, lzy, repeated) =>
        name must_== "simpleTpeRefExample"
        lzy must_== false
        repeated must_== false
    }
  }

  def tpeRefParseLazy = {
    TpeRef apply lazyTpeRefExample match {
      case TpeRef(name, lzy, repeated) =>
        name must_== "lazyTpeRefExample"
        lzy must_== true
        repeated must_== false
    }
  }

  def tpeRefParseArray = {
    TpeRef apply arrayTpeRefExample match {
      case TpeRef(name, lzy, repeated) =>
        name must_== "arrayTpeRefExample"
        lzy must_== false
        repeated must_== true
    }
  }

  def tpeRefParseLazyArray = {
    TpeRef apply lazyArrayTpeRefExample match {
      case TpeRef(name, lzy, repeated) =>
        name must_== "lazyArrayTpeRefExample"
        lzy must_== true
        repeated must_== true
    }
  }

  def multiLineDoc = {
    Field parse multiLineDocExample match {
      case Field(name, doc, tpe, since, default) =>
        name must_== "multiLineDocField"
        doc must_== List("A field whose documentation",
                              "spans over multiple lines")

      case _ =>
        true must_== false
    }
  }

}
