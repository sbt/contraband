package com.example

import sjsonnew.JsonFormat
import sjsonnew.support.scalajson.unsafe.Converter


object Example extends App {
  import Codec.GreetingFormat
  val g0: interfaces.Greeting = new SimpleGreeting("Hello")
  val g1: interfaces.Greeting = new SimpleGreeting("Hello", 0)
  val g21: interfaces.Greeting = new GreetingWithAttachments(Array.empty, "Hello")

  println(Converter.toJson(g0).get)

  assert(Converter.fromJson(Converter.toJson(g0).get).get == g0)
  assert(Converter.fromJson(Converter.toJson(g1).get).get == g1)
  assert(Converter.fromJson(Converter.toJson(g21).get).get == g21)
}
