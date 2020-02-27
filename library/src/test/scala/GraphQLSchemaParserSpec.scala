package sbt.contraband

import org.parboiled2.Position
import parser.SchemaParser
import ast._
import org.scalatest._
import scala.util.Success
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphQLSchemaParserSpec extends AnyFlatSpec with Matchers with Inside {
  "SchemaParser" should "parse an empty type" in {
    val Success(ast) = SchemaParser.parse("""type Hello {}""")
    // println(ast)
    inside(ast) { case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) :: Nil, _, _, _) =>
      name shouldEqual "Hello"
    }
  }

  it should "parse two types" in {
    val Success(ast) = SchemaParser.parse(
      """# comment
        |type Foo {}
        |type Bar {}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, foo :: bar :: Nil, _, _, _) =>
      inside(foo) { case ObjectTypeDefinition(name, _, Nil, Nil, _, comment :: Nil, _, _) =>
        name shouldEqual "Foo"
        comment.text shouldEqual " comment"
      }
      inside(bar) { case ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) =>
        name shouldEqual "Bar"
      }
    }
  }

  it should "parse a type with doc comments" in {
    val Success(ast) = SchemaParser.parse(
      """## doc comment
        |type Foo {}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, _, comment :: Nil, _, _) :: Nil, _, _, _) =>
      name shouldEqual "Foo"
      inside(comment) { case DocComment(x, _) =>
        x shouldEqual " doc comment"
      }
    }
  }

  it should "parse a type with target directive" in {
    val Success(ast) = SchemaParser.parse(
      """type Foo @target(Java) {
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, ObjectTypeDefinition(name, _, Nil, Nil, dir :: Nil, _, _, _) :: Nil, _, _, _) =>
      name shouldEqual "Foo"
      inside(dir) { case Directive(name, Argument(argName, v, _, _) :: Nil, _, _) =>
        name shouldEqual "target"
        argName shouldEqual None
        inside(v) { case EnumValue(value, _, _) =>
          value shouldEqual "Java"
        }
      }
    }
  }

  it should "parse a type with fields" in {
    val Success(ast) = SchemaParser.parse(
      """type Person {
        |  name: String
        |  age: Int!
        |  xs: [java.util.Date]
        |  x: raw"Map[String, String]"
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, foo :: Nil, _, _, _) =>
      inside(foo) { case ObjectTypeDefinition(name, _, Nil, f1 :: f2 :: f3 :: f4 :: Nil, _, _, _, _) =>
        name shouldEqual "Person"
        inside(f1) { case FieldDefinition(name, NamedType(typeName :: Nil, _), _ , _, _, _, _) =>
          name shouldEqual "name"
          typeName shouldEqual "String"
        }
        inside(f2) { case FieldDefinition(name, NotNullType(NamedType(typeName :: Nil, _), _), _, _, _, _, _) =>
          name shouldEqual "age"
          typeName shouldEqual "Int"
        }
        inside(f3) { case FieldDefinition(name, ListType(NamedType(typeName, _), _), _, _, _, _, _) =>
          name shouldEqual "xs"
          typeName shouldEqual List("java", "util", "Date")
        }
        inside(f4) { case FieldDefinition(name, NamedType(typeName, _), _, _, _, _, _) =>
          name shouldEqual "x"
          typeName shouldEqual List("Map[String, String]")
        }
      }
    }
  }

  it should "parse a type with fields with defaults" in {
    val Success(ast) = SchemaParser.parse(
      """type Person {
        |  name: String = "x"
        |  age: Int! = 0
        |  xs: [java.util.Date] = []
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, foo :: Nil, _, _, _) =>
      inside(foo) { case ObjectTypeDefinition(name, _, Nil, f1 :: f2 :: f3 :: Nil, _, _, _, _) =>
        name shouldEqual "Person"
        inside(f1) { case FieldDefinition(name, NamedType(typeName :: Nil, _), _ , Some(StringValue(v, _, _)), _, _, _) =>
          name shouldEqual "name"
          typeName shouldEqual "String"
          v shouldEqual "x"
        }
        inside(f2) { case FieldDefinition(name, NotNullType(NamedType(typeName :: Nil, _), _), _, Some(BigIntValue(v, _, _)), _, _, _) =>
          name shouldEqual "age"
          typeName shouldEqual "Int"
          v shouldEqual 0
        }
        inside(f3) { case FieldDefinition(name, ListType(NamedType(typeName, _), _), _, Some(ListValue(xs, _, _)), _, _, _) =>
          name shouldEqual "xs"
          typeName shouldEqual List("java", "util", "Date")
          xs shouldEqual Nil
        }
      }
    }
  }

  it should "parse a type with fields with since directive" in {
    val Success(ast) = SchemaParser.parse(
      """type Person {
        |  name: String @since("0.1.0")
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, foo :: Nil, _, _, _) =>
      inside(foo) { case ObjectTypeDefinition(name, _, Nil, f1 :: Nil, _, _, _, _) =>
        name shouldEqual "Person"
        inside(f1) { case FieldDefinition(name, NamedType(typeName :: Nil, _), Nil, _, dir :: Nil, _, _) =>
          name shouldEqual "name"
          typeName shouldEqual "String"
          inside(dir) { case Directive(name, Argument(argName, v, _, _) :: Nil, _, _) =>
            name shouldEqual "since"
            argName shouldEqual None
            inside(v) { case StringValue(value, _, _) =>
              value shouldEqual "0.1.0"
            }
          }
        }
      }
    }
  }

  it should "parse an interface" in {
    val Success(ast) = SchemaParser.parse(
      """interface Entity {}
        |
        |type Person implements Entity {
        |  age: Int
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, intf :: person :: Nil, _, _, _) =>
      inside(intf) { case InterfaceTypeDefinition(name, _, _, _, _, _, _, _) =>
        name shouldEqual "Entity"
      }
      inside(person) { case ObjectTypeDefinition(name, _, NamedType(parentTypeName :: Nil, _) :: Nil, f1 :: Nil, _, _, _, _) =>
        name shouldEqual "Person"
        parentTypeName shouldEqual "Entity"
      }
    }
  }

  it should "parse an interface with a parent" in {
    val Success(ast) = SchemaParser.parse(
      """interface Fruit {}
        |
        |interface Citrus implements Fruit {
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, fruit :: citrus :: Nil, _, _, _) =>
      inside(fruit) { case InterfaceTypeDefinition(name, _, _, _, _, _, _, _) =>
        name shouldEqual "Fruit"
      }
      inside(citrus) { case InterfaceTypeDefinition(name, _, NamedType(parentTypeName :: Nil, _) :: Nil, _, _, _, _, _) =>
        name shouldEqual "Citrus"
        parentTypeName shouldEqual "Fruit"
      }
    }
  }

  it should "parse an enumueration" in {
    val Success(ast) = SchemaParser.parse(
      """enum Episode {
        |  NEW_HOPE
        |  EMPIRE
        |  JEDI
        |}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(_, episode :: Nil, _, _, _) =>
      inside(episode) { case EnumTypeDefinition(name, ns, v1 :: v2 :: v3 :: Nil, _, _, _, _) =>
        name shouldEqual "Episode"
        v1.name shouldEqual "NEW_HOPE"
        v2.name shouldEqual "EMPIRE"
        v3.name shouldEqual "JEDI"
      }
    }
  }

  it should "parse a type with package" in {
    val Success(ast) = SchemaParser.parse(
      """package com.example @target(Scala)
        |
        |type Foo {}""".stripMargin)
    // println(ast)
    inside(ast) { case Document(Some(pkg), ObjectTypeDefinition(name, _, Nil, Nil, _, _, _, _) :: Nil, _, _, _) =>
      name shouldEqual "Foo"
      inside(pkg) { case PackageDecl(names, dir :: Nil, _, _) =>
        names shouldEqual List("com", "example")
      }
    }
  }
}
