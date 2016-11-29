---
out: json.html
---

JSON コーデックの生成
-------------------

`JsonCodecPlugin` をサブプロジェクトに追加することで Contraband 型に対する sjson-new の JSON コーデックが生成される。

```scala
lazy val root = (project in file(".")).
  enablePlugins(ContrabandPlugin, JsonCodecPlugin).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies += "com.eed3si9n" %% "sjson-new-scalajson" % "0.4.1" )
```

sjson-new はコーデック・ツールキットで、一つのコーデック定義から Spray JSON の AST、SLIP-28 Scala JSON、MessagePack と複数のバックエンドをサポートすることができる。

コーデックのパッケージ名は `@codecPackage` アノテーションによって指定される。

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

JsonFormat を実装するトレイトはこの場合 `com.example.codec` パッケージ内に生成され、
`CustomJsonProtocol` という名前で全てのトレイトをミックスインしたフルコーデックが生成される。

生成された JSON コーデックはこのように使うことができる:

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
