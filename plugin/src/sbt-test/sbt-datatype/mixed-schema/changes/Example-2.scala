package com.example

class Example {
  val g0: interfaces.Greeting = new SimpleGreeting("Hello")
  val g1: interfaces.Greeting = new SimpleGreeting("Hello", 0)
  val g21: interfaces.Greeting = new GreetingWithAttachments("Hello", Array.empty)
}
