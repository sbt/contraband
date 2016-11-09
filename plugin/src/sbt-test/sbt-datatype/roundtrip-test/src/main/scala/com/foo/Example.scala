package com.foo

import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.Converter
import com.example._

object Example extends App {
  import generated.CustomProtocol._
  val g0: Greeting = new SimpleGreeting("Hello")
  val g1: Greeting = new SimpleGreeting("Hello", 0)
  val g21: Greeting = new GreetingWithAttachments("Hello", Array.empty)
  val g3: Greeting = GreetingWithOption("Hello", Some("foo"))

  println(Converter.toJson(g0).get)

  assert(Converter.fromJson[Greeting](Converter.toJson(g0).get).get == g0)
  assert(Converter.fromJson[Greeting](Converter.toJson(g1).get).get == g1)
  assert(Converter.fromJson[Greeting](Converter.toJson(g21).get).get == g21)
  assert(Converter.fromJson[Greeting](Converter.toJson(g3).get).get == g3)
}
