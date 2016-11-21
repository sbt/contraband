package com.example;

public class Example {
  private com.example.interfaces.Greeting g0 = new SimpleGreeting("Hello");
  private com.example.interfaces.Greeting g1 = new SimpleGreeting("Hello", 0);
  private com.example.interfaces.Greeting g2 = new GreetingWithAttachments("Hello", new java.io.File[0]);
}
