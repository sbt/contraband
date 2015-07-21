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
          "type": "string"
        },
        {
          "name": "header",
          "doc": "The header of the Greeting",
          "type": "GreetingHeader",

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
          "type": "java.util.Date",
          "default": "new java.util.Date()"
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
          "type": "string",
          "default": "Unknown"
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

}
