package com.example

class Example {
  val g0: interfaces.Greeting = SimpleGreeting("Hello")
  val g1: interfaces.Greeting = SimpleGreeting("Hello", 0)
  val g21: interfaces.Greeting = new GreetingWithAttachments("Hello", Array.empty)
}
