package sbt.contraband

import org.scalatest._
import JsonSchemaExample._
import ast._
import parser.JsonParser
import AstUtil._

class JsonSchemaSpec extends FlatSpec with Matchers with Inside {
  "Document.parse should" should "parse empty schemas" in {
    val s = JsonParser.Document.parse(emptySchemaExample)
    assert(s.definitions === Nil)
  }
  it should "parse complete example" in {
    val s = JsonParser.Document.parse(completeExample)
    assert(s.definitions.size === 7)
  }

  "TypeDefinitions.parse" should "parse interface" in {
    val List(x) = JsonParser.TypeDefinitions.parse(emptyInterfaceExample)
    inside(x) { case ast.InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "emptyInterfaceExample"
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      namespace shouldEqual None
      toDoc(comments) shouldEqual Nil
      fields shouldEqual Nil
      val toStringImpl = toToStringImpl(x)
      toStringImpl shouldEqual Nil
      // (abstractMethods.size === 0) &&
      // (children.size === 0) &&
      // (extra === List()) &&
      // (extraCompanion === Nil) &&
      // (parents === Nil) &&
      // (parentsCompanion === Nil))
    }
  }
  it should "parse record" in {
    val List(x) = JsonParser.TypeDefinitions.parse(emptyRecordExample)
    inside(x) { case ObjectTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "emptyRecordExample"
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      namespace shouldEqual None
      toDoc(comments) shouldEqual Nil
      fields shouldEqual Nil
      val toStringImpl = toToStringImpl(x)
      toStringImpl shouldEqual Nil
      // (extra === List()) &&
      // (extraCompanion === Nil) &&
      // (parents === Nil) &&
      // (parentsCompanion === Nil)
    }
  }
  it should "parse enumeration" in {
    val List(x) = JsonParser.TypeDefinitions.parse(emptyEnumerationExample)
    inside(x) { case EnumTypeDefinition(name, namespace, values, dirs, comments, _, _) =>
      name shouldEqual "emptyEnumerationExample"
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      namespace shouldEqual None
      toDoc(comments) shouldEqual Nil
      values shouldEqual Nil
      // (values.size === 0)) // &&
      //(extra === List()))
    }
  }
  it should "throw an error on invalid definition kind" in {
    assertThrows[RuntimeException] {
      val List(x) = JsonParser.TypeDefinitions.parse(invalidDefinitionKindExample)
    }
  }

  "InterfaceTypeDefinition.parse" should "parse simple interface" in {
    val List(x) = JsonParser.InterfaceTypeDefinition.parseInterface(simpleInterfaceExample)
    inside(x) { case ast.InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "simpleInterfaceExample"
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      namespace shouldEqual None
      val doc = toDoc(comments)
      doc shouldEqual List("example of simple interface")
      fields.size shouldEqual 1
      val toStringImpl = toToStringImpl(x)
      toStringImpl shouldEqual List("return \"custom\";")
      //   (fields.size === 1) &&
      //   (fields(0) === Field("field", Nil, TpeRef("type", false, false, false), Field.emptyVersion, None)) &&
      //   (abstractMethods.size === 0) &&
      //   (children.size === 0) &&
      //   (extra === List("// Some extra code...")) &&
      //   (extraCompanion === List("// Some extra companion code...")) &&
      //   (parents === List("Interface1", "Interface2")) &&
      //   (parentsCompanion === List("CompanionInterface1", "CompanionInterface2")))
      inside(fields(0)) { case ast.FieldDefinition(name, fieldType, arguments, defaultValue, directives, comments, position) =>
        name shouldEqual "field"
        fieldType shouldEqual NotNullType(NamedType("type" :: Nil, None))
      }
    }
  }

  it should "parse interface with one child" in {
    val List(x1, x2) = JsonParser.InterfaceTypeDefinition.parseInterface(oneChildInterfaceExample)
    inside(x1) { case ast.InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "oneChildInterfaceExample"
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      namespace shouldEqual None
      val doc = toDoc(comments)
      doc shouldEqual List("example of interface")
      // (fields.size ===1) &&
      // (fields(0) === Field("field", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None)) &&
      // (abstractMethods.size === 0) &&
      // (children.size === 1) &&
      // (extra === List()) &&
      // (toString === None) &&
      // (extraCompanion === Nil) &&
      // (parents === Nil) &&
      // (parentsCompanion === Nil))
    }
    inside(x2) { case ObjectTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "childRecord"
      namespace shouldEqual None
      fields.size shouldEqual 2
      val target = toTarget(dirs)
      target shouldEqual Some("Scala")
      // Field("x", Nil, TpeRef("int", false, false, false), Field.emptyVersion, None) :: Nil, Nil, None, Nil, Nil, Nil)) &&
      interfaces shouldEqual List(NamedType(List("oneChildInterfaceExample"), None))
    }
  }
  it should "parse nested interfaces" in {
    val List(x1, x2) = JsonParser.InterfaceTypeDefinition.parseInterface(nestedInterfaceExample)
    inside(x1) { case ast.InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "nestedProtocolExample"
      //     assert((name === "nestedProtocolExample") &&
      //     (target === "Scala") &&
      //     (namespace === None) &&
      //     (doc === List("example of nested protocols")) &&
      //     (fields.size === 0) &&
      //     (abstractMethods.size === 0) &&
    }
    inside(x2) { case ast.InterfaceTypeDefinition(name, namespace, interfaces, fields, dirs, comments, trailingComments, position) =>
      name shouldEqual "nestedProtocol"
      //     (children(0) === Interface("nestedProtocol", "Scala", None, VersionNumber("0.0.0"), Nil, Nil, Nil, Nil, Nil, None, Nil, Nil, Nil)) &&
      //     (extra === List()) &&
      //     (toString === None) &&
      //     (extraCompanion === Nil) &&
      //     (parents === Nil) &&
      //     (parentsCompanion === Nil))
    }
  }

  "Record.parse" should "parse simple record" in {
    inside(JsonParser.ObjectTypeDefinition.parse(simpleRecordExample)) {
      case ObjectTypeDefinition(name, namespace, interfaces, fields, directives, comments, trailingComments, position) =>
        name shouldEqual "simpleRecordExample"
        // (target === "Scala") &&
        namespace shouldEqual None
        // (doc === List("Example of simple record")) &&
        // (fields.size === 1) &&
        // (fields(0) === Field("field", Nil, TpeRef("java.net.URL", false, false, false), Field.emptyVersion, None)) &&
        // (extra === List("// Some extra code...")) &&
        // (toString === None) &&
        // (extraCompanion === Nil) &&
        // (parents === Nil) &&
        // (parentsCompanion === Nil)
    }
  }

  "Enumeration.parse" should "parse simple enumeration" in {
    JsonParser.EnumTypeDefinition.parse(simpleEnumerationExample) match {
      case e@EnumTypeDefinition(name, namespace, values, directives, comments, _, _) =>
        val doc = toDoc(comments)
        val target = toTarget(directives)
        val extra = toExtra(e)
        assert((name === "simpleEnumerationExample") &&
        (target === Some("Scala")) &&
        (namespace === None) &&
        (doc === List("Example of simple enumeration")) &&
        (values.size === 2) &&
        (values(0) === EnumValueDefinition("first", Nil, List(DocComment("First symbol")), None)) &&
        (values(1) === EnumValueDefinition("second", Nil, Nil, None)) &&
        (extra === List("// Some extra code...")))
    }
  }

  "FieldDefinition.parseMessage" should "parse" in {
    val msg = JsonParser.FieldDefinition.parseMessage(messageExample)
    inside(msg) { case FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) =>
      name shouldEqual "messageExample"
      val doc = toDoc(comments)
      doc shouldEqual List("Example of a message")
      fieldType shouldEqual NotNullType(NamedType("int" :: Nil, None), None)
      inside(arguments) { case List(InputValueDefinition(name, valueType, defaultValue, dirs, comments, _)) =>
        name shouldEqual "arg0"
        valueType shouldEqual NotNullType(NamedType("type2" :: Nil, None), None)
      }
    }
  }

  "FieldDefinition.parse" should "parse" in {
    val field = JsonParser.FieldDefinition.parse(fieldExample)
    inside(field) { case FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) =>
      name shouldEqual "fieldExample"
      val doc = toDoc(comments)
      doc shouldEqual List("Example of field")
      fieldType shouldEqual NotNullType(NamedType("type" :: Nil, None), None)
      val since = getSince(dirs)
      since shouldEqual VersionNumber("1.0.0")
      defaultValue shouldEqual Some(RawValue("2 + 2", Nil, None))
    }
  }

  it should "parse multiline doc comments" in {
    val field = JsonParser.FieldDefinition.parse(multiLineDocExample)
    inside(field) { case FieldDefinition(name, fieldType, arguments, defaultValue, dirs, comments, _) =>
      name shouldEqual "multiLineDocField"
      val doc = toDoc(comments)
      doc shouldEqual List("A field whose documentation", "spans over multiple lines")
    }
  }

  "Type.parse" should "parse simple types" in {
    val tpe = JsonParser.Type.parse(simpleTpeRefExample)
    inside(tpe) { case NotNullType(NamedType(name :: Nil, None), None) =>
      name shouldEqual "simpleTpeRefExample"
    }
  }

  it should "parse lazy types" in {
    val tpe = JsonParser.Type.parse(lazyTpeRefExample)
    inside(tpe) { case LazyType(NotNullType(NamedType(name :: Nil, None), None), None) =>
      name shouldEqual "lazyTpeRefExample"
    }
  }

  it should "parse array types" in {
    val tpe = JsonParser.Type.parse(arrayTpeRefExample)
    inside(tpe) { case ListType(NamedType(name :: Nil, None), None) =>
      name shouldEqual "arrayTpeRefExample"
    }
  }

  it should "parse option types" in {
    val tpe = JsonParser.Type.parse(optionTpeRefExample)
    inside(tpe) { case NamedType(name :: Nil, None) =>
      name shouldEqual "optionTpeRefExample"
    }
  }

  it should "parse lazy array types" in {
    val tpe = JsonParser.Type.parse(lazyArrayTpeRefExample)
    inside(tpe) { case LazyType(ListType(NamedType(name :: Nil, None), None), None) =>
      name shouldEqual "lazyArrayTpeRefExample"
    }
  }

  it should "parse lazy option types" in {
    val tpe = JsonParser.Type.parse(lazyOptionTpeRefExample)
    inside(tpe) { case LazyType(NamedType(name :: Nil, None), None) =>
      name shouldEqual "lazyOptionTpeRefExample"
    }
  }
}
