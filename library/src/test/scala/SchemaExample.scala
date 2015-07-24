package sbt.datatype

object SchemaExample {
  val basicSchema = """{
  "namespace": "com.example",
  "protocol": "HelloWorld",
  "doc": "Protocol Greetings",

  "types": [
    {"name": "Greeting", "type": "record", "fields": [
      {"name": "message", "type": "string"}]}
  ]
}"""

  val growableSchema = """{
  "namespace": "com.example",
  "protocol": "HelloWorld",
  "doc": "Protocol Greetings",

  "types": [
    {"name": "Greeting", "type": "record", "fields": [
      {"name": "message", "type": "string", "since": "0.1.0"},
      {"name": "name", "type": "string", "since": "0.2.0", "default": "foo" }]}
  ]
}"""

}

object NewSchema {

  val emptySchemaExample = """{
  "namespace": "com.example"
}"""

  val emptyProtocolExample = """{
  "type": "protocol",
  "name": "emptyProtocolExample"
}"""

  val emptyRecordExample = """{
  "type": "record",
  "name": "emptyRecordExample"
}"""

  val emptyEnumerationExample = """{
  "type": "enumeration",
  "name": "emptyEnumerationExample"
}"""

  val invalidDefinitionKindExample = """{
  "type": "invalid",
  "name": "hello"
}"""

  val simpleProtocolExample = """{
  "type": "protocol",
  "doc": "example of simple protocol",
  "name": "simpleProtocolExample",
  "fields": [
    {
      "name": "field",
      "type": "type"
    }
  ]
}"""

  val oneChildProtocolExample = """{
  "name": "oneChildProtocolExample",
  "type": "protocol",
  "doc": "example of protocol",
  "types": [
    {
      "name": "childRecord",
      "type": "record"
    }
  ]
}"""

  val nestedProtocolExample = """{
  "name": "nestedProtocolExample",
  "type": "protocol",
  "doc": "example of nested protocols",
  "types": [
    {
      "name": "nestedProtocol",
      "type": "protocol"
    }
  ]
}"""

  val simpleRecordExample = """{
  "name": "simpleRecordExample",
  "type": "record",
  "doc": "Example of simple record",
  "fields": [
    {
      "name": "field",
      "type": "type"
    }
  ]
}"""

  val simpleEnumerationExample = """{
  "name": "simpleEnumerationExample",
  "type": "enumeration",
  "doc": "Example of simple enumeration",
  "types": [
    {
      "name": "first",
      "doc": "First type"
    },
    "second"
  ]
}"""

  val fieldExample = """{
  "name": "fieldExample",
  "doc": "Example of field",
  "type": "type",
  "since": "1.0.0",
  "default": "2 + 2"
}"""

  val simpleTpeRefExample = "simpleTpeRefExample"
  val lazyTpeRefExample = "lazy lazyTpeRefExample"
  val arrayTpeRefExample = "arrayTpeRefExample*"
  val lazyArrayTpeRefExample = "lazy lazyArrayTpeRefExample*"

  val completeExample = """{
  "namespace": "com.example",

  "types": [
    {
      "name": "Greetings",
      "doc": "A greeting protocol",
      "type": "protocol",

      "fields": [
        {
          "name": "message",
          "doc": "The message of the Greeting",
          "type": "lazy string"
        },
        {
          "name": "header",
          "doc": "The header of the Greeting",
          "type": "GreetingHeader",
          "default": "new GreetingHeader(new java.util.Date(), \"Unknown\")",
          "since": "0.2.0"
        }
      ],

      "types": [
        {
          "name": "SimpleGreeting",
          "type": "record",
          "doc": "A Greeting in its simplest form"
        },
        {
          "name": "GreetingWithAttachments",
          "type": "record",
          "doc": "A Greeting with attachments",

          "fields": [
            {
              "name": "attachments",
              "type": "java.io.File*",
              "doc": "The files attached to the greeting"
            }
          ]
        }
      ]
    },
    {
      "name": "GreetingHeader",
      "type": "record",
      "doc": "Meta information of a Greeting",

      "fields": [
        {
          "name": "created",
          "doc": "Creation date",
          "type": "lazy java.util.Date"
        },
        {
          "name": "priority",
          "doc": "The priority of this Greeting",
          "type": "PriorityLevel",
          "since": "0.3.0",
          "default": "PriorityLevel.Medium"
        },
        {
          "name": "author",
          "doc": "The author of the Greeting",
          "type": "string"
        }
      ]
    },
    {
      "name": "PriorityLevel",
      "type": "enumeration",
      "doc": "Priority levels",

      "types": [
        "Low",
        {
          "name": "Medium",
          "doc": "Default priority level"
        },
        "High"
      ]
    }
  ]
}"""

  val completeExampleCode =
    """package com.example
      |/** A greeting protocol */
      |sealed abstract class Greetings(
      |  _message: => String,
      |  /** The header of the Greeting */
      |  val header: GreetingHeader)  {
      |  def this(message: => String) = this(message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |
      |  /** The message of the Greeting */
      |  lazy val message: String = _message
      |  override def equals(o: Any): Boolean = o match {
      |    case x: Greetings => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode
      |  }
      |  override def toString: String = {
      |    "Greetings(" + message + ", " + header + ")"
      |  }
      |}
      |
      |/** A Greeting in its simplest form */
      |final class SimpleGreeting(
      |  message: => String,
      |  header: GreetingHeader) extends Greetings(message, header) {
      |  def this(message: => String) = this(message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |
      |  override def equals(o: Any): Boolean = o match {
      |    case x: SimpleGreeting => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode
      |  }
      |  override def toString: String = {
      |    "SimpleGreeting(" + message + ", " + header + ")"
      |  }
      |}
      |
      |object SimpleGreeting {
      |  def apply(message: => String): SimpleGreeting = new SimpleGreeting(message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |  def apply(message: => String, header: GreetingHeader): SimpleGreeting = new SimpleGreeting(message, header)
      |}
      |
      |/** A Greeting with attachments */
      |final class GreetingWithAttachments(
      |  message: => String,
      |  header: GreetingHeader,
      |  /** The files attached to the greeting */
      |  val attachments: Array[java.io.File]) extends Greetings(message, header) {
      |  def this(message: => String, attachments: Array[java.io.File]) = this(message, new GreetingHeader(new java.util.Date(), "Unknown"), attachments)
      |
      |  override def equals(o: Any): Boolean = o match {
      |    case x: GreetingWithAttachments => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode
      |  }
      |  override def toString: String = {
      |    "GreetingWithAttachments(" + message + ", " + header + ", " + attachments + ")"
      |  }
      |}
      |
      |object GreetingWithAttachments {
      |  def apply(message: => String, attachments: Array[java.io.File]): GreetingWithAttachments = new GreetingWithAttachments(message, new GreetingHeader(new java.util.Date(), "Unknown"), attachments)
      |  def apply(message: => String, header: GreetingHeader, attachments: Array[java.io.File]): GreetingWithAttachments = new GreetingWithAttachments(message, header, attachments)
      |}
      |
      |/** Meta information of a Greeting */
      |final class GreetingHeader(
      |  _created: => java.util.Date,
      |  /** The priority of this Greeting */
      |  val priority: PriorityLevel,
      |  /** The author of the Greeting */
      |  val author: String)  {
      |  def this(created: => java.util.Date, author: String) = this(created, PriorityLevel.Medium, author)
      |
      |  /** Creation date */
      |  lazy val created: java.util.Date = _created
      |  override def equals(o: Any): Boolean = o match {
      |    case x: GreetingHeader => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode
      |  }
      |  override def toString: String = {
      |    "GreetingHeader(" + created + ", " + priority + ", " + author + ")"
      |  }
      |}
      |
      |object GreetingHeader {
      |  def apply(created: => java.util.Date, author: String): GreetingHeader = new GreetingHeader(created, PriorityLevel.Medium, author)
      |  def apply(created: => java.util.Date, priority: PriorityLevel, author: String): GreetingHeader = new GreetingHeader(created, priority, author)
      |}
      |
      |/** Priority levels */
      |sealed abstract class PriorityLevel
      |object PriorityLevel {
      |
      |  case object Low extends PriorityLevel
      |  /** Default priority level */
      |  case object Medium extends PriorityLevel
      |
      |  case object High extends PriorityLevel
      |}""".stripMargin

}
