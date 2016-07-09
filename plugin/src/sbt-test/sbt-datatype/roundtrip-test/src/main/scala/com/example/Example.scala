package com.example

import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.Converter
import interfaces.Greeting

object Example extends App {
  import CustomProtocol._
  val g0: Greeting = new SimpleGreeting("Hello")
  val g1: Greeting = new SimpleGreeting("Hello", 0)
  val g21: Greeting = new GreetingWithAttachments(Array.empty, "Hello")

  println(Converter.toJson(g0).get)

  assert(Converter.fromJson[Greeting](Converter.toJson(g0).get).get == g0)
  assert(Converter.fromJson[Greeting](Converter.toJson(g1).get).get == g1)
  assert(Converter.fromJson[Greeting](Converter.toJson(g21).get).get == g21)
}
