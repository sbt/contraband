---
out: schema.html
---

Schemas and Types
-----------------

This page describes Contraband type system, which is based on GraphQL type system.

Contraband could be used to acess existing JSON-based API,
or to implement your own service.

### Contraband schema language

Since we don't want to rely on a specific programming language syntax,
to talk about Contraband schemas, we'll extend GraphQL's schema language.

### Record types and fields

The most basic components of a Contraband schema are record types, which just represent a kind of object you can fetch from your service, and what fields it has. In the Contraband schema language, we might represent it like this:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
type Character {
  name: String!
  appearsIn: [com.example.Episode]!
}
```

Let's go over it so that we can have a shared vocabulary:

- `com.example` is a package name for this schema. This package name will be used for the generated code.
- The code generation will target Scala by default.
- `##` denotes the document comment for the record type.
- `Character` is a Contraband record type, meaning it's a type with some fields. Most of the types in your schema will be record types.
- `name` and `appearsIn` are fields on the `Character` type. That means that `name` and `appearsIn` are the only fields that can appear in the JSON data of the `Character` type.
- `String` is one of the built-in scalar types.
- `String!` means that the field is required, meaning that the service promises to always give you a value when you query this field. In the type language, we'll represent those with an exclamation mark.
- `[Episode]!` represents a list of `Episode` objects. Since it is also required, you can always expect an array (with zero or more items) when you query the appearsIn field.

Now you know what a Contraband record type looks like, and how to read the basics of the Contraband schema language.

### since directive

To enable schema evolution,
fields in a Contraband record can declare when it was added:

```
package com.example
@target(Scala)

type Greeting {
  message: String!
  x: Int @since("0.2.0")
}
```

This means that `message` field has been around since the beginning ("0.0.0") but optional `x` field was added since version `"0.2.0"`.
Contraband will generate multiple constructors to maintain the binary compatibility.

Since `Int` is optional, `None` is used as the default value.
To supply some other default value, you can write it as follows:

```
package com.example
@target(Scala)

type Greeting {
  message: String!
  x: Int = 0 @since("0.2.0")
}
```

### Scalar types

Contraband comes with a set of default scalar types out of the box:

- `String`
- `Boolean`
- `Byte`
- `Char`
- `Int`
- `Long`
- `Short`
- `Double`

You can also use Java and Scala class names such as 'java.io.File'.

This case, you would have to also supply how the type should be serialized and deserialized.

### Enumeration types

Also called Enums, enumeration types are a special kind of scalar that is restricted to a particular set of allowed values. This allows you to:

1. Validate that any arguments of this type are one of the allowed values.
2. Communicate through the type system that a field will always be one of a finite set of values.

Here's what an enum definition might look like in the Contraband schema language:

```
package com.example
@target(Scala)

## Star Wars trilogy.
enum Episode {
  NewHope
  Empire
  Jedi
}
```

This means that wherever we use the type `Episode` in our schema, we expect it to be exactly one of `NewHope`, `Empire`, or `Jedi`.

### Required type

Record types and enums are the only kinds of types you can define in Contraband. But when you use the types in other parts of the schema,
you can apply additional type modifiers that affect validation of those values. Let's look at an example:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
type Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
}
```

Here, we're using a `String` type and marking it as Required by adding an exclamation mark, `!` after the type name.

### List type

Lists work in a similar way: We can use a type modifier to mark a
type as a list, which indicates that this field will
return an array of that type. In the schema language,
this is denoted by wrapping the type in square brackets, `[` and `]`.

### Lazy type

Lazy types defer the initialization of the field until it is first used.
In the schema language,
this is denoted by the keyword `lazy`.

### Interfaces

Like many type systems, Contraband supports interfaces. An Interface is an abstract type that includes a certain set of fields that a type must include to implement the interface.

For example, you could have an interface `Character` that represents any character in the Star Wars trilogy:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
interface Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
}
```

This means that any type that *implements* `Character` needs to have these exact fields.

For example, here are some types that might implement `Character`:

```
package com.example
@target(Scala)

type Human implements Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
  starships: [com.example.Starship]
  totalCredits: Int
}

type Droid implements Character {
  name: String!
  appearsIn: [com.example.Episode]!
  friends: lazy [com.example.Character]
  primaryFunction: String
}
```

You can see that both of these types have all of the fields from the `Character` interface, but also bring in extra fields, `totalCredits`, `starships` and `primaryFunction`, that are specific to that particular type of character.

### Messages

In addition to fields, an interface can also declare messages.

```
package com.example
@target(Scala)

## Starship represents the starships in Star Wars.
interface Starship {
  name: String!
  length(unit: com.example.LengthUnit): Double
}
```

This means that any type that *implements* `Starship` needs to have both exact fields and messages.

### Extra code

As an escape hatch to inject Scala or Java code into the generated code
Contraband provides special comment notations.

```
## Example of an interface
interface IntfExample {
  field: Int

  #x // Some extra code

  #xinterface Interface1
  #xinterface Interface2

  #xtostring return "custom";

  #xcompanion // Some extra companion code

  #xcompanioninterface CompanionInterface1
  #xcompanioninterface CompanionInterface2
}
```

- `#x` injects the code into the main body of the generated class.
- `#xinterface` adds additional parent classes.
- `#xtostring` is used to provide a custom `toString` method.
- `#xcompanion` injects the code in the companion object of the generated class.
- `#xcompanioninterface` add additional parent classes to the companion object.
