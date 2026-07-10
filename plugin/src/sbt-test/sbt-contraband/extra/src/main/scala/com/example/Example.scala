package com.example

object Example {
  def main(args: Array[String]): Unit = {
    val name = "Martin"
    val martin = Person(name, 25)
    assert(martin.toUpperCase == Person(name.toUpperCase, 25))
  }
}
