package sbt.datatype

import org.scalatest.FlatSpec
import SchemaExample._

class SchemaSpec extends FlatSpec {
  "ProtocolSchema.parse" should "parse" in {
    val s = ProtocolSchema.parse(basicSchema)
    assert(s.namespace === "com.example")
  }

  "Schema.parse should" should "parse empty Schemas" in {
    val s = Schema parse emptySchemaExample
    assert(s.definitions === Nil)
  }
  it should "parse complete example" in {
    val s = Schema parse completeExample
    assert(s.definitions.size === 3)
  }

  "Definition.parse" should "parse interface" in {
    Definition parse emptyInterfaceExample match {
      case Interface(name, target, namespace, _, doc, fields, abstractMethods, children, extra) =>
        assert((name === "emptyInterfaceExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List()) &&
        (fields.size === 0) &&
        (abstractMethods.size === 0) &&
        (children.size === 0) &&
        (extra === List()))
      case _ =>
        fail()
    }
  }
  it should "parse record" in {
    Definition parse emptyRecordExample match {
      case Record(name, target, namespace, _, doc, fields, extra) =>
        assert((name === "emptyRecordExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List()) &&
        (fields.size === 0) &&
        (extra === List()))
      case _ =>
        fail()
    }
  }
  it should "parse enumeration" in {
    Definition parse emptyEnumerationExample match {
      case Enumeration(name, target, namespace, _, doc, values, extra) =>
        assert((name === "emptyEnumerationExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List()) &&
        (values.size === 0) &&
        (extra === List()))
      case _ =>
        fail()
    }
  }
  it should "throw an error on invalid definition kind" in {
    assertThrows[RuntimeException] {
      Definition parse invalidDefinitionKindExample
    }
  }

  "Interface.parse" should "parse simple interface" in {
    Interface parse simpleInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        assert((name === "simpleInterfaceExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List("example of simple interface")) &&
        (fields.size === 1) &&
        (fields(0) === Field("field", Nil, TpeRef("type", false, false, false), Field.emptyVersion, None)) &&
        (abstractMethods.size === 0) &&
        (children.size === 0) &&
        (extra === List("// Some extra code...")))
    }
  }

  it should "parse interface with one child" in {
    Interface parse oneChildInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        assert((name === "oneChildInterfaceExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List("example of interface")) &&
        (fields.size ===1) &&
        (fields(0) === Field("field", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None)) &&
        (abstractMethods.size === 0) &&
        (children.size === 1) &&
        (children(0) === Record("childRecord", "Scala", None, VersionNumber("0.0.0"), Nil,
          Field("x", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None) :: Nil, Nil)) &&
        (extra === List()))
    }
  }
  it should "parse nested interfaces" in {
    Interface parse nestedInterfaceExample match {
      case Interface(name, target, namespace, since, doc, fields, abstractMethods, children, extra) =>
        assert((name === "nestedProtocolExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List("example of nested protocols")) &&
        (fields.size === 0) &&
        (abstractMethods.size === 0) &&
        (children.size === 1) &&
        (children(0) === Interface("nestedProtocol", "Scala", None, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil, Nil)) &&
        (extra === List()))
    }
  }

  "Record.parse" should "parse simple record" in {
    Record parse simpleRecordExample match {
      case Record(name, target, namespace, since, doc, fields, extra) =>
        assert((name === "simpleRecordExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List("Example of simple record")) &&
        (fields.size === 1) &&
        (fields(0) === Field("field", Nil, TpeRef("java.net.URL", false, false, false), Field.emptyVersion, None)) &&
        (extra === List("// Some extra code...")))
    }
  }

  "Enumeration.parse" should "parse simple enumeration" in {
    Enumeration parse simpleEnumerationExample match {
      case Enumeration(name, target, namespace, since, doc, values, extra) =>
        assert((name === "simpleEnumerationExample") &&
        (target === "Scala") &&
        (namespace === None) &&
        (doc === List("Example of simple enumeration")) &&
        (values.size === 2) &&
        (values(0) === EnumerationValue("first", List("First symbol"))) &&
        (values(1) === EnumerationValue("second", Nil)) &&
        (extra === List("// Some extra code...")))
    }
  }

  "Message.parse" should "parse" in {
    Message parse messageExample match {
      case Message(name, doc, retTpe, request) =>
        assert((name === "messageExample") &&
        (doc === List("Example of a message")) &&
        (retTpe === TpeRef("int", false, false, false)) &&
        (request === List(Request("arg0", Nil, TpeRef("type2", false, false, false)))))
    }
  }

  "Field.parse" should "parse" in {
    Field parse fieldExample match {
      case Field(name, doc, tpe, since, default) =>
        assert((name === "fieldExample") &&
        (doc === List("Example of field")) &&
        (tpe === TpeRef("type", false, false, false)) &&
        (since === VersionNumber("1.0.0")) &&
        (default === Some("2 + 2")))
    }
  }

  it should "parse multiline doc comments" in {
    Field parse multiLineDocExample match {
      case Field(name, doc, tpe, since, default) =>
        assert((name === "multiLineDocField") &&
        (doc === List("A field whose documentation",
                              "spans over multiple lines")))
      case _ =>
        fail()
    }
  }

  "TpeRef.apply" should "parse simple types" in {
    TpeRef apply simpleTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "simpleTpeRefExample") &&
        (lzy === false) &&
        (repeated === false) &&
        (opt === false))
    }
  }

  it should "parse lazy types" in {
    TpeRef apply lazyTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "lazyTpeRefExample") &&
        (lzy === true) &&
        (repeated === false) &&
        (opt === false))
    }
  }

  it should "parse array types" in {
    TpeRef apply arrayTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "arrayTpeRefExample") &&
        (lzy === false) &&
        (repeated === true) &&
        (opt === false))
    }
  }

  it should "parse option types" in {
    TpeRef apply optionTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "optionTpeRefExample") &&
        (lzy === false) &&
        (repeated === false) &&
        (opt === true))
    }
  }

  it should "parse lazy array types" in {
    TpeRef apply lazyArrayTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "lazyArrayTpeRefExample") &&
        (lzy === true) &&
        (repeated === true) &&
        (opt === false))
    }
  }

  it should "parse lazy option types" in {
    TpeRef apply lazyOptionTpeRefExample match {
      case TpeRef(name, lzy, repeated, opt) =>
        assert((name === "lazyOptionTpeRefExample") &&
        (lzy === true) &&
        (repeated === false) &&
        (opt === true))
    }
  }
}
