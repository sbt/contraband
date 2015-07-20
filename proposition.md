# sbt-datatype

## Current example of the schema

```javascript
{
  "namespace": "com.example",
  "protocol": "HelloWorld",
  "doc": "Protocol Greetings",
  "types": [
    {"name": "Greeting", "type": "record", "fields": [
      {"name": "message", "type": "string", "since": "0.1.0"},
      {"name": "name", "type": "string", "since": "0.2.0", "default": "foo" }]}
  ]
}
```

## Changes to the schema

 - Subtypes of a `protocol` must be defined within the `protocol`:
```javascript
{
	"name": "Greetings",
	"type": "protocol",
	"doc": "Protocol Greetings",

	"types": [
		{
			"name": "NamedGreetings",
			"type": "record",
			...
		},
		{
			"name": "AnonymousGreetings",
			"type": "record",
			...
		}
	]
}
```
 - `protocol`s can be defined inside `protocol`s
```javascript
{
	"name": "Greetings",
	"type": "protocol",
	"doc": "Protocol Greetings",

	"types": [
		{
			"name": "RemoteGreeting",
			"type": "Protocol",

			"types": [
				{
					...
				}
			]
		}
	]
}
```
 - Support for enumerations
```javascript
{
	"name": "Weekdays",
	"type": "enumeration",
	"doc": "The days of the week",

	"types": [
		"Sunday",
		{
			"doc": "A day that is documented",
			"name: "Monday"
		},
		"Tuesday",
		...,
		"Saturday"
	]
}
```
 - Support for lazy parameters
```javascript
{
	"name": "LogElement",
	"type": "record",

	"fields": [
		{
			"name": "message",
			"type": "~string"
		}
	]
}
```
 - Support for repeated parameters
```javascript
{
	"name": "Parameterized",
	"type": "record",

	"fields": [
		{
			"name": "parameters",
			"type": "Type*"
		}
	]
}
```

## About the `since` member

The `since` member can appear only in the definition of a `field`. It indicates that a field `f` exists in a `protocol` or `record` since the version specified. If a field definition has a `since` member, then one of the two following conditions must hold true:

 - it provides the `default` member, which will tell what is the default value for this `field`
 - an instance of its type can be constructed without explicit parameters

A field without a `since` member is assumed to exist since the first version of the schema.

## About the `default` member

The `default` member can appear only in the definition of a `field`. It indicates the default value for a field (i.e. the value that this field will have when a record is constructed using a deprecated constructor that does not allow setting this field explicitly.) This member can contain simple expressions that **must compile and evaluate to the same value in Java and in Scala**. It is required for fields that have a `since` member and whose type does not provide a default constructor.

```javascript
{
	"types": [
		{
			"name": "ParameterizedType",
			"type": "record",

			"fields": [
				{
					"name": "typeName",
					"type": "string"
				},
				/* This field does not need to specify a default value because
				   `Type` can be constructed without parameters. */
				{
					"name": "parameter",
					"type": "Type",
					"since": "0.2.0"
				}
			]
		},
		{
			"name": "Type",
			"type": "record",
			"fields": [
				"name": "typeName",
				"type": "string"
				"default": "noType"
			]
		}
	]
}
```

## Implementation of the definitions

How does the following schema map to Scala and Java?

```javascript
{
	"namespace": "com.example",

	"types": [
		{
			"name": "Greetings",
			"doc": "A greeting protocol",
			"type": "protocol",

			"fields": [
				{
					"name": "message",
					"doc": "The message of the Greeting"
					"type": "string",
				},
				{
					"name": "header",
					"doc": "The header of the Greeting"
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
					"type": "java.util.Date"
					"default": "new java.util.Date()",
				},
				{
					"name": "priority",
					"doc": "The priority of this Greeting",
					"type": "PriorityLevel",
					"since": "0.3.0"
					"default": "PriorityLevel.Medium"
				},
				{
					"name": "author",
					"doc": "The author of the Greeting"
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
					"doc": "Default priority level",
				},
				"High"
			]
		}
	]
}
```

### In Scala

```scala
package com.example

/** A greeting protocol */
sealed abstract class Greetings(
  /** The message of the Greeting */
  val message: String,
  /** The header of the Greeting */
  val header: GreetingHeader) {

  def this(message: String) = this(message, new GreetingHeader)
}

/** A Greeting in its simplest form */
final class SimpleGreeting(message: String, header: GreetingHeader) extends Greetings(message, header) {
  def this(message: String) = this(message, new GreetingHeader)
}

/** A Greeting with attachments */
final class GreetingWithAttachments(message: String, header: GreetingHeader,
  /** The files attached to the greeting */
  val attachments: Array[java.io.File]) {

  def this(message: String, attachments: Array[java.io.File]) = this(message, new GreetingHeader, attachments)
}

final class GreetingHeader(
  /** Creation date */
  val created: java.util.Date,
  /** The priority of this Greeting */
  val priority: PriorityLevel,
  /** The author of the Greeting */
  val author: String) {

  def this() = this(new java.util.Date(), PriorityLevel.Medium, "Unknown")
}

/** Priority levels */
sealed trait PriorityLevel
object PriorityLevel {
  case object Low extends PriorityLevel
  /** Default priority level */
  case object Medium extends PriorityLevel
  case object High extends PriorityLevel
}
```

### In java

```java
package com.example;

/** A greeting protocol */
public abstract class Greetings {
	/** The message of the Greeting */
	private final String message;

	/** The header of the Greeting */
	private final GreetingHeader header;

	/** The message of the Greeting */
	public String message() {
		return this.message;
	}

	/** The header of the Greeting */
	public GreetingHeader header() {
		return this.header;
	}

	public Greetings(String message, GreetingHeader header) {
		this.message = message;
		this.header = header;
	}

	public Greetings(String message) {
		this(message, new GreetingHeader());
	}
}

/** A Greeting in its simplest form */
public final class SimpleGreeting extends Greetings {

	public SimpleGreeting(String message) {
		this(message, new GreetingHeader());
	}

	public SimpleGreeting(String message, GreetingHeader header) {
		super(message, header);
	}
}

/** A Greeting with attachments */
public final class GreetingWithAttachments extends Greetings {
	/** The files attached to the greeting */
	private final java.io.File[] attachments;

	public GreetingWithAttachments(String message, java.io.File[] attachments) {
		this(message, new GreetingHeader(), attachments);
	}

	public GreetingWithAttachments(String message, GreetingHeader header, java.io.File[] attachments) {
		super(message, header);
		this.attachments = attachments;
	}

	public java.io.File[] attachments() {
		return this.attachments;
	}
}

/** Meta information of a Greeting */
public final class GreetingHeader {
	/** Creation date */
	private final java.util.Date created;
	/** The priority of this Greeting */
	private final PriorityLevel priority;
	/** The author of the Greeting */
	private final String author;

	public GreetingHeader() {
		 this.created = new java.util.Date();
		 this.priority = PriorityLevel.Medium;
		 this.author = "Unknown";
	}

	public GreetingHeader(java.util.Date created, PriorityLevel priority, String author) {
		this.created = created;
		this.priority = priority;
		this.author = author;
	}

	public java.util.Date created() {
		return this.created;
	}

	public PriorityLevel priority() {
		return this.priority;
	}

	public String author() {
		return this.author;
	}
}

/** Priority levels */
public enum PriorityLevel {
	Low,
	/** Default priority level */
	Medium,
	High
}
```

## Issues
 - Arrays break immutability. Should we use something else instead?
