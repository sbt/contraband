---
out: code-generation.html
---

コード生成
---------

このページでは、Contraband の型システムが Java と Scala でどうエンコーディングされるのかを解説する。

### レコード型

レコード型は Java や Scala ではクラスとして変換され、Scala に標準の case class に相当する。

標準の case class は最初に使い始めるのは便利だが、バイナリ互換性を保ったままフィールドを追加することができない。
Contraband のレコード (疑似 case class と言うこともできる) は、case class とほぼ同様の機能を提供しつつバイナリ互換性を保ったままフィールドの追加を可能としている。

```
package com.example
@target(Scala)

type Person {
  name: String!
  age: Int
}
```

このスキーマは以下の Scala コードを生成する:

```scala
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
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
  private[this] def copy(name: String = name, age: Option[Int] = age): Person = {
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

標準の case class と違って、Contraband のレコードは、
互換性を保ったまま進化できるように `unapply` や public な `copy` メソッドは実装しない。

`copy` の代わりにそれぞれのフィールドに対して `withX(...)` メソッドが生成される。

```scala
> val x = Person("Alice", 20)
> x.withAge(21)
```

Java のコード生成は以下のようになっている (target アノテーションを Java に変更する):

```java
/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package com.example;
public final class Person implements java.io.Serializable {
    
    public static Person create(String _name, java.util.Optional<Integer> _age) {
        return new Person(_name, _age);
    }
    public static Person of(String _name, java.util.Optional<Integer> _age) {
        return new Person(_name, _age);
    }
    public static Person create(String _name, int _age) {
        return new Person(_name, _age);
    }
    public static Person of(String _name, int _age) {
        return new Person(_name, _age);
    }
    
    private String name;
    private java.util.Optional<Integer> age;
    protected Person(String _name, java.util.Optional<Integer> _age) {
        super();
        name = _name;
        age = _age;
    }
    protected Person(String _name, int _age) {
        super();
        name = _name;
        age = java.util.Optional.<Integer>ofNullable(_age);
    }
    public String name() {
        return this.name;
    }
    public java.util.Optional<Integer> age() {
        return this.age;
    }
    public Person withName(String name) {
        return new Person(name, age);
    }
    public Person withAge(java.util.Optional<Integer> age) {
        return new Person(name, age);
    }
    public Person withAge(int age) {
        return new Person(name, java.util.Optional.<Integer>ofNullable(age));
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
        return 37 * (37 * (37 * (17 + "com.example.Person".hashCode()) + name().hashCode()) + age().hashCode());
    }
    public String toString() {
        return "Person("  + "name: " + name() + ", " + "age: " + age() + ")";
    }
}
```

