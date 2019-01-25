
Contraband
==========

Contraband is a description language for your datatypes and APIs,
currently targeting Java and Scala.

You describe the types and fields of your API,
and Contraband will:

- generate either Java classes, or a pseudo case classes in Scala.
- generate JSON bindings for the datatypes.

Contraband also enables you to evolve the API over time.

### Setup

To enable the Contraband plugin for your build, put the following line in `project/contraband.sbt`:

```scala
addSbtPlugin("org.scala-sbt" % "sbt-contraband" % "X.Y.Z")
```

Your Contraband schema should be placed in `src/main/contraband` and `src/test/contraband`.
Here’s how your build should be configured:

```scala
lazy val library = (project in file("library")).
  enablePlugins(ContrabandPlugin).
  settings(
    name := "foo library"
  )
```

### Note

Contraband is NOT supported under the Lightbend subscription.


Schemas and Types
-----------------

This page describes Contraband type system, which is based on GraphQL type system.

Contraband could be used to acess existing JSON-based API,
or to implement your own service.

### Contraband schema language

Since we don't want to rely on a specific programming language syntax,
to talk about Contraband schemas, we'll extend GraphQL's schema language.

A Contraband schema should be saved with the file extension `*.contra`.

### Record types and fields

The most basic components of a Contraband schema are record types, which just represent a kind of object you can fetch from your service, and what fields it has. In the Contraband schema language, we might represent it like this:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
type Character {
  name: String!
  appearsIn: [com.example.Episode]!
}
```

Let's go over it so that we can have a shared vocabulary:

- `com.example` is a package name for this schema. This package name will be used for the generated code.
- `@target(Scala)` is an annotation for the package. It means that the code generation will target Scala by default.
- `##` denotes the document comment for the record type.
- `Character` is a Contraband record type, meaning it's a type with some fields. Most of the types in your schema will be record types. In Java and Scala it is encoded as a class.
- `name` and `appearsIn` are fields on the `Character` type. That means that `name` and `appearsIn` are the only fields that can appear in the JSON object of the `Character` type.
- `String` is one of the built-in scalar types.
- `String!` means that the field is required, meaning that the service promises to always give you a value when you query this field. In the schema language, we'll represent those with an exclamation mark.
- `[Episode]!` represents a list of `Episode` records. Since it is also required, you can always expect a list (with zero or more items) when you query the `appearsIn` field.

Now you know what a Contraband record type looks like, and how to read the basics of the Contraband schema language.

### since annotation

To enable schema evolution,
fields in a Contraband record can declare the version in which it was added:

```
package com.example
@target(Scala)

type Greeting {
  value: String!
  x: Int @since("0.2.0")
}
```

This means that `value` field has been around since the beginning (`"0.0.0"`) but optional `x` field was added since version `"0.2.0"`.
Contraband will generate multiple constructors to maintain the binary compatibility.

Since `Int` is optional, `None` is used as the default value of `x`.
To supply some other default value, you can write it as follows:

```
package com.example
@target(Scala)

type Greeting {
  value: String!
  x: Int = 0 @since("0.2.0")
  p: Person = { name: "Foo" } @since("0.2.0")
  z: Person = raw"Person(\"Foo\")"
}
```

Note that `0` will automatically wrapped with options.

### Scalar types

Contraband comes with a set of default scalar types out of the box:

- `String`
- `Boolean`
- `Byte`
- `Char`
- `Int`
- `Long`
- `Short`
- `Double`

You can also use Java and Scala class names such as `java.io.File`.

If you use class names such as `java.io.File`, you would have to also supply how the type should be serialized and deserialized.

### Enumeration types

Also called Enums, enumeration types are a special kind of scalar that is restricted to a particular set of allowed values. This allows you to:

1. Validate that any arguments of this type are one of the allowed values.
2. Communicate through the type system that a field will always be one of a finite set of values.

Here's what an enum definition might look like in the Contraband schema language:

```
package com.example
@target(Scala)

## Star Wars trilogy.
enum Episode {
  NewHope
  Empire
  Jedi
}
```

This means that wherever we use the type `Episode` in our schema, we expect it to be exactly one of `NewHope`, `Empire`, or `Jedi`.

### Required type

Record types and enums are the only kinds of types you can define in Contraband. But when you use the types in other parts of the schema, you can apply additional type modifiers that affect validation of those values. Let's look at an example:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
type Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
}
```

Here, we're using a `String` type and marking it as Required by adding an exclamation mark, `!` after the type name.

### List type

Lists work in a similar way: We can use a type modifier to mark a
type as a list, which indicates that this field will
return a list of that type. In the schema language,
this is denoted by wrapping the type in square brackets, `[` and `]`.

### Lazy type

Lazy types defer the initialization of the field until it is first used.
In the schema language,
this is denoted by the keyword `lazy`.

### Interfaces

Like many type systems, Contraband supports interfaces. An Interface is an abstract type that includes a certain set of fields that a type must include to *implement* the interface.

For example, you could have an interface `Character` that represents any character in the Star Wars trilogy:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
interface Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
}
```

This means that any type that *implements* `Character` needs to have these exact fields.

For example, here are some types that might implement `Character`:

```
package com.example
@target(Scala)

type Human implements Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
  starships: [com.example.Starship]
  totalCredits: Int
}

type Droid implements Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
  primaryFunction: String
}
```

You can see that both of these types have all of the fields from the `Character` interface, but also bring in extra fields, `totalCredits`, `starships` and `primaryFunction`, that are specific to that particular type of character.

### Messages

In addition to fields, an interface can also declare messages.

```
package com.example
@target(Scala)

## Starship represents the starships in Star Wars.
interface Starship {
  name: String!
  length(unit: com.example.LengthUnit): Double
}
```

This means that any type that *implements* `Starship` needs to have both exact fields and messages.

### Extra code

As an escape hatch to inject Scala or Java code into the generated code
Contraband provides special comment notations.

```
## Example of an interface
interface IntfExample {
  field: Int

  #x // Some extra code

  #xinterface Interface1
  #xinterface Interface2

  #xtostring return "custom";

  #xcompanion // Some extra companion code

  #xcompanioninterface CompanionInterface1
  #xcompanioninterface CompanionInterface2
}
```

- `#x` injects the code into the main body of the generated class.
- `#xinterface` adds additional parent classes.
- `#xtostring` is used to provide a custom `toString` method.
- `#xcompanion` injects the code in the companion object of the generated class.
- `#xcompanioninterface` add additional parent classes to the companion object.


Code generation
---------------

This page describes how the Contraband type system is encoded in Java and Scala.

### Record types

Record types are mapped to Java or Scala classes, corresponding to the standard case classes in Scala.

While the standard case class is convenient to start, it is not possible to add new fields without breaking binary compatibility.
The Contraband records (or pseudo case classes) allow you to add new fields without breaking binary compatibility while offering (almost) the same functionalities as plain case classes.

```
package com.example
@target(Scala)

type Person {
  name: String!
  age: Int
}
```

This schema will produce the following Scala class:

```scala
/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example
final class Person private (
  val name: String,
  val age: Option[Int]) extends Serializable {
  override def equals(o: Any): Boolean = o match {
    case x: Person => (this.name == x.name) && (this.age == x.age)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + name.##) + age.##)
  }
  override def toString: String = {
    "Person(" + name + ", " + age + ")"
  }
  private[this] def copy(name: String = name, age: Option[Int] = age): Person = {
    new Person(name, age)
  }
  def withName(name: String): Person = {
    copy(name = name)
  }
  def withAge(age: Option[Int]): Person = {
    copy(age = age)
  }
  def withAge(age: Int): Person = {
    copy(age = Option(age))
  }
}
object Person {
  def apply(name: String, age: Option[Int]): Person = new Person(name, age)
  def apply(name: String, age: Int): Person = new Person(name, Option(age))
}
```

Unlike the standard case class the Contraband record does not implement `unapply` or public `copy` method,
which cannot evolve in a binary compatible way.

Instead of `copy` it generates `withX(...)` methods for each field.

```scala
> val x = Person("Alice", 20)
> x.withAge(21)
```

Here's the Java code it generates (after changing the target annotation to `Java`):

```java
/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example;
public final class Person implements java.io.Serializable {
    
    public static Person create(String _name, java.util.Optional<Integer> _age) {
        return new Person(_name, _age);
    }
    public static Person of(String _name, java.util.Optional<Integer> _age) {
        return new Person(_name, _age);
    }
    public static Person create(String _name, int _age) {
        return new Person(_name, _age);
    }
    public static Person of(String _name, int _age) {
        return new Person(_name, _age);
    }
    
    private String name;
    private java.util.Optional<Integer> age;
    protected Person(String _name, java.util.Optional<Integer> _age) {
        super();
        name = _name;
        age = _age;
    }
    protected Person(String _name, int _age) {
        super();
        name = _name;
        age = java.util.Optional.<Integer>ofNullable(_age);
    }
    public String name() {
        return this.name;
    }
    public java.util.Optional<Integer> age() {
        return this.age;
    }
    public Person withName(String name) {
        return new Person(name, age);
    }
    public Person withAge(java.util.Optional<Integer> age) {
        return new Person(name, age);
    }
    public Person withAge(int age) {
        return new Person(name, java.util.Optional.<Integer>ofNullable(age));
    }
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Person)) {
            return false;
        } else {
            Person o = (Person)obj;
            return name().equals(o.name()) && age().equals(o.age());
        }
    }
    public int hashCode() {
        return 37 * (37 * (37 * (17 + "com.example.Person".hashCode()) + name().hashCode()) + age().hashCode());
    }
    public String toString() {
        return "Person("  + "name: " + name() + ", " + "age: " + age() + ")";
    }
}
```



JSON codec generation
---------------------


Adding `JsonCodecPlugin` to the subproject will generate sjson-new JSON codes for the Contraband types.

```scala
lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % contrabandSjsonNewVersion.value
  )
```

sjson-new is a codec toolkit that lets you define a code that supports Spray JSON's AST, SLIP-28 Scala JSON, and MessagePack as the backend.

The package name for the codecs can be specified using `@codecPackage` directive.

```
package com.example
@target(Scala)
@codecPackage("com.example.codec")
@codecTypeField("type")
@fullCodec("CustomJsonProtocol")

type Person {
  name: String!
  age: Int
}
```

JsonFormat traits will be generated under `com.example.codec` package, along with a full codec named `CustomJsonProtocol` that mixes in all the traits.

Here's how the generated JSON codec can be used:

```scala
scala> import sjsonnew.support.scalajson.unsafe.{ Converter, CompactPrinter, Parser }
import sjsonnew.support.scalajson.unsafe.{Converter, CompactPrinter, Parser}

scala> import com.example.codec.CustomJsonProtocol._
import com.example.codec.CustomJsonProtocol._

scala> import com.example.Person
import com.example.Person

scala> val p = Person("Bob", 20)
p: com.example.Person = Person(Bob, 20)

scala> val j = Converter.toJsonUnsafe(p)
j: scala.json.ast.unsafe.JValue = JObject([Lscala.json.ast.unsafe.JField;@6731ad72)

scala> val s = CompactPrinter(j)
s: String = {"name":"Bob","age":20}

scala> val x = Parser.parseUnsafe(s)
x: scala.json.ast.unsafe.JValue = JObject([Lscala.json.ast.unsafe.JField;@7331f7f8)

scala> val q = Converter.fromJsonUnsafe[Person](x)
q: com.example.Person = Person(Bob, 20)

scala> assert(p == q)
```

### Skipping codec generation

Use the `@generateCodec(false)` annotation to skip the codec generation for some types.

```
interface MiddleInterface implements InterfaceExample
@generateCodec(false)
{
  field: Int
}
```
