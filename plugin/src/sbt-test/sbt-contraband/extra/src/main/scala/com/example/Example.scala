package com.example

object Example extends App {
  val name = "Martin"
  val martin = Person(name, 25)
  assert(martin.toUpperCase == Person(name.toUpperCase, 25))
}
