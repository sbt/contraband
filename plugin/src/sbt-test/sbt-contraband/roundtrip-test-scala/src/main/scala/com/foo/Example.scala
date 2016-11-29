package com.foo

import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.{ Converter, CompactPrinter }
import com.example._

object Example extends App {
  import codec.CustomJsonProtocol._
  val g0: Greeting = SimpleGreeting("Hello")
  val g1: Greeting = SimpleGreeting("Hello", None)
  val g21: Greeting = GreetingWithAttachments("Hello", Vector.empty)
  val g3: Greeting = GreetingWithOption("Hello", Option("foo"))

  val json = CompactPrinter(Converter.toJson(g0).get)
  println(json)
  assert(json == """{"$type":"SimpleGreeting","message":"Hello"}""")
  println(Converter.fromJson[Greeting](Converter.toJson(g0).get).get)

  assert(Converter.fromJson[Greeting](Converter.toJson(g0).get).get == g0)
  assert(Converter.fromJson[Greeting](Converter.toJson(g1).get).get == g1)
  assert(Converter.fromJson[Greeting](Converter.toJson(g21).get).get == g21)


  val json3 = CompactPrinter(Converter.toJson(g3).get)
  println(json3)
  assert(json3 == """{"$type":"GreetingWithOption","message":"Hello","opt":"foo"}""")
  println(Converter.fromJson[Greeting](Converter.toJson(g3).get).get)
  assert(Converter.fromJson[Greeting](Converter.toJson(g3).get).get == g3)
}
