package sbt.contraband

import java.io.File

object GraphQLExample {
  val simpleEnumerationExample = """
package com.example @target(Scala)

## Example of an enumeration
enum EnumExample {
  ## First symbol
  First
  Second

  #x def extra: String = ???
}"""

  val recordExample = """
package com.example @target(Scala)

## Example of a type
type TypeExample {
  field: java.net.URL

  #x // Some extra code
}"""

  val intfExample = """
package com.example @target(Scala)

## Example of an interface
interface InterfaceExample {
  field: Int

  #x // Some extra code
}

type ChildType implements InterfaceExample {
  name: String
  field: Int
}
"""

  val messageExample = """
package com.example @target(Scala) @codecPackage("generated")

interface IntfExample {
  ## I'm a field.
  field: Int

  ## A very simple example of a message.
  ## Messages can only appear in interface definitions.
  messageExample(
    ## The first argument of the message.
    ## Make sure it is awesome.
    arg0: lazy [Int]

    ## This argument is not important, so it gets single line doc.
    arg1: Boolean): [Int]
}

"""
}
