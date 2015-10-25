package sbt.datatype

import java.io.File

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

  val emptySchemaExample = """{}"""

  val emptyProtocolExample = """{
  "type": "protocol",
  "target": "Scala",
  "name": "emptyProtocolExample"
}"""

  val emptyRecordExample = """{
  "type": "record",
  "target": "Scala",
  "name": "emptyRecordExample"
}"""

  val emptyEnumerationExample = """{
  "type": "enumeration",
  "target": "Scala",
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
  "target": "Scala",
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
  "target": "Scala",
  "doc": "example of protocol",
  "types": [
    {
      "name": "childRecord",
      "type": "record",
      "target": "Scala"
    }
  ]
}"""

  val nestedProtocolExample = """{
  "name": "nestedProtocolExample",
  "target": "Scala",
  "type": "protocol",
  "doc": "example of nested protocols",
  "types": [
    {
      "name": "nestedProtocol",
      "target": "Scala",
      "type": "protocol"
    }
  ]
}"""

  val simpleRecordExample = """{
  "name": "simpleRecordExample",
  "type": "record",
  "target": "Scala",
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
  "target": "Scala",
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

  val abstractMethodExample = """{
  "name": "abstractMethodExample",
  "doc": "Example of abstract method",
  "type": "type",
  "args": [
    {
      "name": "arg0",
      "type": "type2"
    }
  ]
}"""

  val simpleTpeRefExample = "simpleTpeRefExample"
  val lazyTpeRefExample = "lazy lazyTpeRefExample"
  val arrayTpeRefExample = "arrayTpeRefExample*"
  val lazyArrayTpeRefExample = "lazy lazyArrayTpeRefExample*"

  val primitiveTypesExample = """{
  "types": [
    {
      "name": "primitiveTypesExample",
      "target": "Scala",
      "type": "record",
      "fields": [
        {
          "name": "simpleInteger",
          "type": "int"
        },
        {
          "name": "lazyInteger",
          "type": "lazy int"
        },
        {
          "name": "arrayInteger",
          "type": "int*"
        },
        {
          "name": "lazyArrayInteger",
          "type": "lazy int*"
        }
      ]
    }
  ]
}"""

  val primitiveTypesNoLazyExample = """{
  "types": [
    {
      "name": "primitiveTypesNoLazyExample",
      "target": "Scala",
      "type": "record",
      "fields": [
        {
          "name": "simpleInteger",
          "type": "int"
        },
        {
          "name": "arrayInteger",
          "type": "int*"
        }
      ]
    }
  ]
}"""

  val generateArgDocExample = """{
  "types": [
    {
      "name": "generateArgDocExample",
      "target": "Scala",
      "type": "protocol",
      "fields": [
        {
          "name": "field",
          "type": "int",
          "doc": "I'm a field."
        }
      ],
      "methods": [
        {
          "name": "methodExample",
          "doc": [
            "A very simple example of abstract method.",
            "Abstract methods can only appear in protocol definitions."
          ],
          "type": "int*",
          "args": [
            {
              "name": "arg0",
              "type": "lazy int*",
              "doc": [
                "The first argument of the method.",
                "Make sure it is awesome."
              ]
            },
            {
              "name": "arg1",
              "type": "boolean",
              "doc": "This argument is not important, so it gets single line doc."
            }
          ]
        }
      ]
    }
  ]
}"""

  val completeExample = """{
  "types": [
    {
      "name": "Greetings",
      "namespace": "com.example",
      "target": "Scala",
      "doc": "A greeting protocol",
      "type": "protocol",

      "fields": [
        {
          "name": "message",
          "doc": [
            "The message of the Greeting",
            "This is a multiline doc comment"
          ],
          "type": "lazy String"
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
          "namespace": "com.example",
          "target": "Scala",
          "type": "record",
          "doc": "A Greeting in its simplest form"
        },
        {
          "name": "GreetingWithAttachments",
          "namespace": "com.example",
          "target": "Scala",
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
      "namespace": "com.example",
      "target": "Scala",
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
          "type": "String"
        }
      ]
    },
    {
      "name": "PriorityLevel",
      "namespace": "com.example",
      "target": "Scala",
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

  val completeExampleCodeScala =
    """package com.example
      |/** A greeting protocol */
      |sealed abstract class Greetings(
      |  _message: => String,
      |  /** The header of the Greeting */
      |  val header: GreetingHeader) extends Serializable {
      |  def this(message: => String) = this(message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |
      |  /**
      |   * The message of the Greeting
      |   * This is a multiline doc comment
      |   */
      |  lazy val message: String = _message
      |  override def equals(o: Any): Boolean = o match {
      |    case x: Greetings => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
      |  }
      |  override def toString: String = {
      |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
      |  }
      |}
      |
      |/** A Greeting in its simplest form */
      |final class SimpleGreeting(
      |  message: => String,
      |  header: GreetingHeader) extends com.example.Greetings(message, header) {
      |  def this(message: => String) = this(message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |
      |  override def equals(o: Any): Boolean = o match {
      |    case x: SimpleGreeting => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
      |  }
      |  override def toString: String = {
      |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
      |  }
      |  private[this] def copy(message: => String = message, header: GreetingHeader = header): SimpleGreeting = {
      |    new SimpleGreeting(message, header)
      |  }
      |  def withMessage(message: => String): SimpleGreeting = {
      |    copy(message = message)
      |  }
      |  def withHeader(header: GreetingHeader): SimpleGreeting = {
      |    copy(header = header)
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
      |  /** The files attached to the greeting */
      |  val attachments: Array[java.io.File],
      |  message: => String,
      |  header: GreetingHeader) extends com.example.Greetings(message, header) {
      |  def this(attachments: Array[java.io.File], message: => String) = this(attachments, message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |
      |  override def equals(o: Any): Boolean = o match {
      |    case x: GreetingWithAttachments => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
      |  }
      |  override def toString: String = {
      |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
      |  }
      |  private[this] def copy(attachments: Array[java.io.File] = attachments, message: => String = message, header: GreetingHeader = header): GreetingWithAttachments = {
      |    new GreetingWithAttachments(attachments, message, header)
      |  }
      |  def withAttachments(attachments: Array[java.io.File]): GreetingWithAttachments = {
      |    copy(attachments = attachments)
      |  }
      |  def withMessage(message: => String): GreetingWithAttachments = {
      |    copy(message = message)
      |  }
      |  def withHeader(header: GreetingHeader): GreetingWithAttachments = {
      |    copy(header = header)
      |  }
      |}
      |
      |object GreetingWithAttachments {
      |  def apply(attachments: Array[java.io.File], message: => String): GreetingWithAttachments = new GreetingWithAttachments(attachments, message, new GreetingHeader(new java.util.Date(), "Unknown"))
      |  def apply(attachments: Array[java.io.File], message: => String, header: GreetingHeader): GreetingWithAttachments = new GreetingWithAttachments(attachments, message, header)
      |}
      |
      |/** Meta information of a Greeting */
      |final class GreetingHeader(
      |  _created: => java.util.Date,
      |  /** The priority of this Greeting */
      |  val priority: PriorityLevel,
      |  /** The author of the Greeting */
      |  val author: String) extends Serializable {
      |  def this(created: => java.util.Date, author: String) = this(created, PriorityLevel.Medium, author)
      |
      |  /** Creation date */
      |  lazy val created: java.util.Date = _created
      |  override def equals(o: Any): Boolean = o match {
      |    case x: GreetingHeader => super.equals(o) // We have lazy members, so use object identity to avoid circularity.
      |    case _ => false
      |  }
      |  override def hashCode: Int = {
      |    super.hashCode // Avoid evaluating lazy members in hashCode to avoid circularity.
      |  }
      |  override def toString: String = {
      |    super.toString // Avoid evaluating lazy members in toString to avoid circularity.
      |  }
      |  private[this] def copy(created: => java.util.Date = created, priority: PriorityLevel = priority, author: String = author): GreetingHeader = {
      |    new GreetingHeader(created, priority, author)
      |  }
      |  def withCreated(created: => java.util.Date): GreetingHeader = {
      |    copy(created = created)
      |  }
      |  def withPriority(priority: PriorityLevel): GreetingHeader = {
      |    copy(priority = priority)
      |  }
      |  def withAuthor(author: String): GreetingHeader = {
      |    copy(author = author)
      |  }
      |}
      |
      |object GreetingHeader {
      |  def apply(created: => java.util.Date, author: String): GreetingHeader = new GreetingHeader(created, PriorityLevel.Medium, author)
      |  def apply(created: => java.util.Date, priority: PriorityLevel, author: String): GreetingHeader = new GreetingHeader(created, priority, author)
      |}
      |
      |/** Priority levels */
      |sealed abstract class PriorityLevel extends Serializable
      |object PriorityLevel {
      |
      |  case object Low extends PriorityLevel
      |  /** Default priority level */
      |  case object Medium extends PriorityLevel
      |
      |  case object High extends PriorityLevel
      |}""".stripMargin

  val completeExampleCodeJava =
    Map(
      new File("com/example/GreetingHeader.java") ->
        """package com.example;
          |/** Meta information of a Greeting */
          |public final class GreetingHeader implements java.io.Serializable {
          |
          |    /** Creation date */
          |    private com.example.MyLazy<java.util.Date> created;
          |    /** The priority of this Greeting */
          |    private PriorityLevel priority;
          |    /** The author of the Greeting */
          |    private String author;
          |
          |    public GreetingHeader(com.example.MyLazy<java.util.Date> _created, String _author) {
          |        super();
          |        created = _created;
          |        priority = PriorityLevel.Medium;
          |        author = _author;
          |    }
          |
          |    public GreetingHeader(com.example.MyLazy<java.util.Date> _created, PriorityLevel _priority, String _author) {
          |        super();
          |        created = _created;
          |        priority = _priority;
          |        author = _author;
          |    }
          |
          |    public java.util.Date created() {
          |        return this.created.get();
          |    }
          |
          |    public PriorityLevel priority() {
          |        return this.priority;
          |    }
          |
          |    public String author() {
          |        return this.author;
          |    }
          |
          |    public GreetingHeader withCreated(com.example.MyLazy<java.util.Date> created) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public GreetingHeader withPriority(PriorityLevel priority) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public GreetingHeader withAuthor(String author) {
          |        return new GreetingHeader(created, priority, author);
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,

      new File("com/example/PriorityLevel.java") ->
        """package com.example;
          |/** Priority levels */
          |public enum PriorityLevel {
          |    Low,
          |    /** Default priority level */
          |    Medium,
          |    High
          |}""".stripMargin,

      new File("com/example/GreetingWithAttachments.java") ->
        """package com.example;
          |/** A Greeting with attachments */
          |public final class GreetingWithAttachments extends com.example.Greetings {
          |
          |    /** The files attached to the greeting */
          |    private java.io.File[] attachments;
          |
          |    public GreetingWithAttachments(java.io.File[] _attachments, com.example.MyLazy<String> _message) {
          |        super(_message, new GreetingHeader(new java.util.Date(), "Unknown"));
          |        attachments = _attachments;
          |    }
          |
          |    public GreetingWithAttachments(java.io.File[] _attachments, com.example.MyLazy<String> _message, GreetingHeader _header) {
          |        super(_message, _header);
          |        attachments = _attachments;
          |    }
          |
          |    public java.io.File[] attachments() {
          |        return this.attachments;
          |    }
          |
          |    public GreetingWithAttachments withAttachments(java.io.File[] attachments) {
          |        return new GreetingWithAttachments(attachments, new com.example.MyLazy<String>() { public String get() { return message(); } }, header());
          |    }
          |
          |    public GreetingWithAttachments withMessage(com.example.MyLazy<String> message) {
          |        return new GreetingWithAttachments(attachments, message, header());
          |    }
          |
          |    public GreetingWithAttachments withHeader(GreetingHeader header) {
          |        return new GreetingWithAttachments(attachments, new com.example.MyLazy<String>() { public String get() { return message(); } }, header);
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,

      new File("com/example/Greetings.java") ->
        """package com.example;
          |/** A greeting protocol */
          |public abstract class Greetings implements java.io.Serializable {
          |
          |    /**
          |     * The message of the Greeting
          |     * This is a multiline doc comment
          |     */
          |    private com.example.MyLazy<String> message;
          |    /** The header of the Greeting */
          |    private GreetingHeader header;
          |
          |    public Greetings(com.example.MyLazy<String> _message) {
          |        super();
          |        message = _message;
          |        header = new GreetingHeader(new java.util.Date(), "Unknown");
          |    }
          |
          |    public Greetings(com.example.MyLazy<String> _message, GreetingHeader _header) {
          |        super();
          |        message = _message;
          |        header = _header;
          |    }
          |
          |    public String message() {
          |        return this.message.get();
          |    }
          |
          |    public GreetingHeader header() {
          |        return this.header;
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin,

      new File("com/example/SimpleGreeting.java") ->
        """package com.example;
          |/** A Greeting in its simplest form */
          |public final class SimpleGreeting extends com.example.Greetings {
          |
          |    public SimpleGreeting(com.example.MyLazy<String> _message) {
          |        super(_message, new GreetingHeader(new java.util.Date(), "Unknown"));
          |    }
          |
          |    public SimpleGreeting(com.example.MyLazy<String> _message, GreetingHeader _header) {
          |        super(_message, _header);
          |    }
          |
          |    public SimpleGreeting withMessage(com.example.MyLazy<String> message) {
          |        return new SimpleGreeting(message, header());
          |    }
          |
          |    public SimpleGreeting withHeader(GreetingHeader header) {
          |        return new SimpleGreeting(new com.example.MyLazy<String>() { public String get() { return message(); } }, header);
          |    }
          |
          |    public boolean equals(Object obj) {
          |        return this == obj; // We have lazy members, so use object identity to avoid circularity.
          |    }
          |
          |    public int hashCode() {
          |        return super.hashCode(); // Avoid evaluating lazy members in hashCode to avoid circularity.
          |    }
          |
          |    public String toString() {
          |        return super.toString(); // Avoid evaluating lazy members in toString to avoid circularity.
          |    }
          |}""".stripMargin)

  val growableAddOneFieldExample = """{
  "name": "growableAddOneField",
  "target": "Scala",
  "type": "record",
  "fields": [
    {
      "name": "field",
      "type": "int",
      "since": "0.1.0",
      "default": "0"
    }
  ]
}""".stripMargin

  val multiLineDocExample =
    """{
      |  "name": "multiLineDocField",
      |  "type": "int",
      |  "doc": [
      |    "A field whose documentation",
      |    "spans over multiple lines"
      |  ]
      |}""".stripMargin

}
