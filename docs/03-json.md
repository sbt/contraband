---
out: json.html
---

JSON codec generation
---------------------


Adding `JsonCodecPlugin` to the subproject will generate sjson-new JSON codes for the datatypes.

```scala
lazy val root = (project in file(".")).
  enablePlugins(DatatypePlugin, JsonCodecPlugin).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.4.1" )
```

The package name for the codecs can be specified using `@codecPackage` directive.

```
package com.example
@target(Scala)
@codecPackage("com.example.codec")
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
