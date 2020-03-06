package sbt.contraband

import org.parboiled2.Position
import parser.SchemaParser
import ast._
import verify._
import scala.util.Success

object GraphQLSchemaParserSpec extends BasicTestSuite {
  test("SchemaParser should parse an empty type") {
    val Success(ast) = SchemaParser.parse("""type Hello {}""")
    // println(ast)
    ast match {
      case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) :: Nil, _, _, _) =>
        assert(name == "Hello")
      case _ => fail()
    }
  }

  test("parse two types") {
    val Success(ast) = SchemaParser.parse("""# comment
        |type Foo {}
        |type Bar {}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, foo :: bar :: Nil, _, _, _) =>
        foo match {
          case ObjectTypeDefinition(name, _, Nil, Nil, _, comment :: Nil, _, _) =>
            assert(name == "Foo")
            assert(comment.text == " comment")
          case _ => fail()
        }
        bar match {
          case ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) =>
            assert(name == "Bar")
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with doc comments") {
    val Success(ast) = SchemaParser.parse("""## doc comment
        |type Foo {}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, _, comment :: Nil, _, _) :: Nil, _, _, _) =>
        assert(name == "Foo")
        comment match {
          case DocComment(x, _) =>
            assert(x == " doc comment")
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with extra comments") {
    val Success(ast) = SchemaParser.parse("""type Foo {
        |  #x // extra code
        |  #xinterface Interface1
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, _, _, List(comment1, comment2), _) :: Nil, _, _, _) =>
        assert(name == "Foo")
        comment1 match {
          case ExtraComment(x, _) => assert(x == " // extra code")
          case _                  => fail(comment1.toString)
        }
        comment2 match {
          case ExtraIntfComment(x, _) => assert(x == "Interface1")
          case _                      => fail(comment2.toString)
        }
      case _ => fail(ast.toString)
    }
  }

  test("parse a type with target directive") {
    val Success(ast) = SchemaParser.parse("""type Foo @target(Java) {
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, dir :: Nil, _, _, _) :: Nil, _, _, _) =>
        assert(name == "Foo")

        dir match {
          case Directive(name, Argument(argName, v, _, _) :: Nil, _, _) =>
            assert(name == "target")
            assert(argName == None)
            v match {
              case EnumValue(value, _, _) =>
                assert(value == "Java")
              case _ => fail()
            }
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with fields") {
    val Success(ast) = SchemaParser.parse("""type Person {
        |  name: String
        |  age: Int!
        |  xs: [java.util.Date]
        |  x: raw"Map[String, String]"
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, foo :: Nil, _, _, _) =>
        foo match {
          case ObjectTypeDefinition(name, _, Nil, f1 :: f2 :: f3 :: f4 :: Nil, _, _, _, _) =>
            assert(name == "Person")
            f1 match {
              case FieldDefinition(name, NamedType(typeName :: Nil, _), _, _, _, _, _) =>
                assert(name == "name")
                assert(typeName == "String")
              case _ => fail()
            }
            f2 match {
              case FieldDefinition(name, NotNullType(NamedType(typeName :: Nil, _), _), _, _, _, _, _) =>
                assert(name == "age")
                assert(typeName == "Int")
              case _ => fail()
            }
            f3 match {
              case FieldDefinition(name, ListType(NamedType(typeName, _), _), _, _, _, _, _) =>
                assert(name == "xs")
                assert(typeName == List("java", "util", "Date"))
              case _ => fail()
            }
            f4 match {
              case FieldDefinition(name, NamedType(typeName, _), _, _, _, _, _) =>
                assert(name == "x")
                assert(typeName == List("Map[String, String]"))
              case _ => fail()
            }
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with fields with defaults") {
    val Success(ast) = SchemaParser.parse("""type Person {
        |  name: String = "x"
        |  age: Int! = 0
        |  xs: [java.util.Date] = []
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, foo :: Nil, _, _, _) =>
        foo match {
          case ObjectTypeDefinition(name, _, Nil, f1 :: f2 :: f3 :: Nil, _, _, _, _) =>
            assert(name == "Person")

            f1 match {
              case FieldDefinition(name, NamedType(typeName :: Nil, _), _, Some(StringValue(v, _, _)), _, _, _) =>
                assert(name == "name")
                assert(typeName == "String")
                assert(v == "x")
              case _ => fail()
            }
            f2 match {
              case FieldDefinition(name, NotNullType(NamedType(typeName :: Nil, _), _), _, Some(BigIntValue(v, _, _)), _, _, _) =>
                assert(name == "age")
                assert(typeName == "Int")
                assert(v == 0)
              case _ => fail()
            }
            f3 match {
              case FieldDefinition(name, ListType(NamedType(typeName, _), _), _, Some(ListValue(xs, _, _)), _, _, _) =>
                assert(name == "xs")
                assert(typeName == List("java", "util", "Date"))
                assert(xs == Nil)
              case _ => fail()
            }
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with fields with since directive") {
    val Success(ast) = SchemaParser.parse("""type Person {
        |  name: String @since("0.1.0")
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, foo :: Nil, _, _, _) =>
        foo match {
          case ObjectTypeDefinition(name, _, Nil, f1 :: Nil, _, _, _, _) =>
            assert(name == "Person")
            f1 match {
              case FieldDefinition(name, NamedType(typeName :: Nil, _), Nil, _, dir :: Nil, _, _) =>
                assert(name == "name")
                assert(typeName == "String")

                dir match {
                  case Directive(name, Argument(argName, v, _, _) :: Nil, _, _) =>
                    assert(name == "since")
                    assert(argName == None)
                    v match {
                      case StringValue(value, _, _) => assert(value == "0.1.0")
                      case _                        => fail()
                    }
                  case _ => fail()
                }
              case _ => fail()
            }
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse an interface") {
    val Success(ast) = SchemaParser.parse("""interface Entity {}
        |
        |type Person implements Entity {
        |  age: Int
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, intf :: person :: Nil, _, _, _) =>
        intf match {
          case InterfaceTypeDefinition(name, _, _, _, _, _, _, _) =>
            assert(name == "Entity")
          case _ => fail()
        }
        person match {
          case ObjectTypeDefinition(name, _, NamedType(parentTypeName :: Nil, _) :: Nil, f1 :: Nil, _, _, _, _) =>
            assert(name == "Person")
            assert(parentTypeName == "Entity")
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse an interface with a parent") {
    val Success(ast) = SchemaParser.parse("""interface Fruit {}
        |
        |interface Citrus implements Fruit {
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, fruit :: citrus :: Nil, _, _, _) =>
        fruit match {
          case InterfaceTypeDefinition(name, _, _, _, _, _, _, _) =>
            assert(name == "Fruit")
          case _ => fail()
        }
        citrus match {
          case InterfaceTypeDefinition(name, _, NamedType(parentTypeName :: Nil, _) :: Nil, _, _, _, _, _) =>
            assert(name == "Citrus")
            assert(parentTypeName == "Fruit")
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse an enumueration") {
    val Success(ast) = SchemaParser.parse("""enum Episode {
        |  NEW_HOPE
        |  EMPIRE
        |  JEDI
        |}""".stripMargin)
    // println(ast)
    ast match {
      case Document(_, episode :: Nil, _, _, _) =>
        episode match {
          case EnumTypeDefinition(name, ns, v1 :: v2 :: v3 :: Nil, _, _, _, _) =>
            assert(name == "Episode")
            assert(v1.name == "NEW_HOPE")
            assert(v2.name == "EMPIRE")
            assert(v3.name == "JEDI")
          case _ => fail()
        }
      case _ => fail()
    }
  }

  test("parse a type with package") {
    val Success(ast) = SchemaParser.parse("""package com.example @target(Scala)
        |
        |type Foo {}""".stripMargin)
    // println(ast)
    ast match {
      case Document(Some(pkg), ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) :: Nil, _, _, _) =>
        assert(name == "Foo")
        pkg match {
          case PackageDecl(names, dir :: Nil, _, _) =>
            assert(names == List("com", "example"))
          case _ => fail()
        }
      case _ => fail()
    }
  }
}
