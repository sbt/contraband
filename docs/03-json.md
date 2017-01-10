---
out: json.html
---

JSON codec generation
---------------------


Adding `JsonCodecPlugin` to the subproject will generate sjson-new JSON codes for the Contraband types.

```scala
lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.7.0"
  )
```

sjson-new is a codec toolkit that lets you define a code that supports Sray JSON's AST, SLIP-28 Scala JSON, and MessagePack as the backend.

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
