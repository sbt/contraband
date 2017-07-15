package com.foo

import java.util.Optional
import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.{ Converter, CompactPrinter }
import com.example._

object Example extends App {
  import generated.CustomProtocol._
  val g0: Greeting = SimpleGreeting("Hello")
  val g1: Greeting = SimpleGreeting("Hello", Optional.empty[java.lang.Integer]())
  val g21: Greeting = GreetingWithAttachments.of("Hello", Array.empty)
  val g3: Greeting = GreetingWithOption("Hello", Optional.ofNullable("foo"))

  println(CompactPrinter(Converter.toJson(g0).get))
  println(Converter.fromJson[Greeting](Converter.toJson(g0).get).get)

  assert(Converter.fromJson[Greeting](Converter.toJson(g0).get).get == g0)
  assert(Converter.fromJson[Greeting](Converter.toJson(g1).get).get == g1)
  assert(Converter.fromJson[Greeting](Converter.toJson(g21).get).get == g21)
  assert(Converter.fromJson[Greeting](Converter.toJson(g3).get).get == g3)
}
