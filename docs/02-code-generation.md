---
out: code-generation.html
---

Code generation
---------------

This page describes how the Contraband type system is encoded in Java and Scala.

### Record types

Record types are mapped to Java or Scala classes, corresponding to the standard case classes in Scala.

While the standard case class is convenient to start, it is not possible to add new fields without breaking binary compatibility.
The Contraband records (or pseudo case classes) allow you to add new fields without breaking binary compatibility while offering (almost) the same functionalities as plain case classes.

```
package com.example
@target(Scala)

type Person {
  name: String!
  age: Int
}
```

This schema will produce the following Scala class:

```scala
/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example
final class Person private (
  val name: String,
  val age: Option[Int]) extends Serializable {
  override def equals(o: Any): Boolean = o match {
    case x: Person => (this.name == x.name) && (this.age == x.age)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + name.##) + age.##)
  }
  override def toString: String = {
    "Person(" + name + ", " + age + ")"
  }
  protected[this] def copy(name: String = name, age: Option[Int] = age): Person = {
    new Person(name, age)
  }
  def withName(name: String): Person = {
    copy(name = name)
  }
  def withAge(age: Option[Int]): Person = {
    copy(age = age)
  }
  def withAge(age: Int): Person = {
    copy(age = Option(age))
  }
}
object Person {
  def apply(name: String, age: Option[Int]): Person = new Person(name, age)
  def apply(name: String, age: Int): Person = new Person(name, Option(age))
}
```

Unlike the standard case class the Contraband record does not implement `unapply` or public `copy` method,
which cannot evolve in a binary compatible way.

Instead of `copy` it generates `withX(...)` methods for each field.

```scala
> val x = Person("Alice", 20)
> x.withAge(21)
```

Here's the Java code it generates (after changing the target annotation to `Java`):

```java
/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example;
public final class Person implements java.io.Serializable {

    private String name;
    private com.example.Maybe<Integer> age;
    public Person(String _name, com.example.Maybe<Integer> _age) {
        super();
        name = _name;
        age = _age;
    }
    public Person(String _name, int _age) {
        super();
        name = _name;
        age = com.example.Maybe.<Integer>just(_age);
    }
    public String name() {
        return this.name;
    }
    public com.example.Maybe<Integer> age() {
        return this.age;
    }
    public Person withName(String name) {
        return new Person(name, age);
    }
    public Person withAge(com.example.Maybe<Integer> age) {
        return new Person(name, age);
    }
    public Person withAge(int age) {
        return new Person(name, com.example.Maybe.<Integer>just(age));
    }
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Person)) {
            return false;
        } else {
            Person o = (Person)obj;
            return name().equals(o.name()) && age().equals(o.age());
        }
    }
    public int hashCode() {
        return 37 * (37 * (17 + name().hashCode()) + age().hashCode());
    }
    public String toString() {
        return "Person("  + "name: " + name() + ", " + "age: " + age() + ")";
    }
}
```

