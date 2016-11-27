---
out: schema.html
---

スキーマと型
-----------

このページでは GraphQL の型システムに基づいた Contraband の型システムを説明する。

Contraband は、既存の JSON ベースの API をアクセスするのに使ったり、サービスを実装するのに使うことができる。

### Contraband スキーマ言語

Contraband のスキーマを記述するのに特定のプログラミング言語の構文に依存したくないため、
GraphQL のスキーマ言語を拡張することにした。

### レコード型とフィールド

Contraband スキーマの最も基本的なものはレコード型で、
サービスから取得するオブジェクトとそのフィールドを表す。
Contraband スキーマ言語では以下のように表現する:

```
package com.example
@target(Scala)

## Character represents the characters in Star Wars.
type Character {
  name: String!
  appearsIn: [com.example.Episode]!
}
```

ボキャブラリーを共有できるように、一つ一つみていこう:

- `com.example` は、このスキーマのパッケージだ。このパッケージ名は生成されるコードにも使われる。
- `@target(Scala)` はこのパッケージに対するアノテーションだ。これは、コード生成がデフォルトで Scala を対象 (target) とすることを意味する。
- `##` は、このレコード型に関するドキュメントコメントのための記法だ。
- `Character` は Contraband のレコード型で、これは何らかのフィールドを持つ型であることを意味する。Java と Scala ではクラスとしてエンコードされる。
- `name` と `appearsIn` は `Character` 型のフィールドだ。これは、`Character` 型の JSON オブジェクトにこれらのフィールドのみを持つことを意味する。
- `String` は組み込みのスカラー型だ。
- `String!` はフィールドが required (省略不可) であることを意味する。つまり、サービスはこのフィールドを必ず返すということだ。スキーマ言語ではこれを感嘆符 (!) で表す。
- `[Episode]!` は `Episode` レコードのリストを表す。これも required であるため、`appearsIn` フィールドは必ずリスト (ゼロ個もしくはそれ以上のアイテム) を返すことが保証される。

これで、Contraband のレコード型がどういうものかと、Contraband スキーマ言語の基本が分かったはずだ。

### since アノテーション

スキーマの進化を可能とするため、Contraband のレコード内のフィールドはどのバージョンでそれが追加されたかを宣言できる:

```
package com.example
@target(Scala)

type Greeting {
  value: String!
  x: Int @since("0.2.0")
}
```

これは、`value` フィールドは最初のバージョン (`"0.0.0"`) から存在したが、`x` フィールドはバージョン `"0.2.0"` にて追加されたことを示す。
Contraband はバイナリ互換性を保つように、この情報を用いて複数のコンストラクタを生成する。

`Int` は optional 型なので、デフォルト値として `None` が自動的に使われる。
他にデフォルト値を設定したければ、以下のように書くことができる:

```
package com.example
@target(Scala)

type Greeting {
  value: String!
  x: Int = 0 @since("0.2.0")
}
```

`0` が自動的に option でラッピングされることに注目してほしい。

### スカラー型

Contraband はデフォルトでいくつかの組み込み型を提供する:

- `String`
- `Boolean`
- `Byte`
- `Char`
- `Int`
- `Long`
- `Short`
- `Double`

`java.io.File` といった Java や Scala のクラス名も使用することができる。

ただし、その場合はその型がどうシリアライズ・デシリアライズされるかの方法も提供する必要がある。

### 列挙型

別名 enum とも呼ばれる列挙型はスカラー型の特殊なもので特定の値の集合に制限されている。これによって

1. この型を持つ引数が許可された値のうちのどれかであることを検査できる。
2. 型システムを用いて、フィールドは常に有限な値の集合のうちどれか一つの値を取るということを明示できる。

Contraband スキーマ言語では enum の定義は以下のように書ける:

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

これは、スキーマ内で `Episode` と書いたとき、`NewHope`, `Empire`, `Jedi`
のどれかの値を取ることを意味する。

### required 型

レコード型と enum は Contraband でユーザが定義することが型だ。
しかし、型をスキーマ内で使うときには型修飾子を付けて値の検査などを変えることができる。
具体例で説明しよう。

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

ここでは、`String` 型名の後に感嘆符 (`!`) を付けて required (省略不可) な型だとマークしている。

### リスト型

リストも同様だ。型修飾子を使って型をリストだとマークして、フィールドがその型のリストを返すことを示す。スキーマ言語では、型を角括弧 (`[` と `]`) で囲むことで表記する。

### lazy 型

lazy 型は、フィールドの初期化を最初に使われるまで遅延する。
スキーマ言語では、`lazy` というキーワードを使ってこれを表す。

### インターフェイス

多くの型システム同様に Contraband もインターフェイスという概念を持つ。
インターフェイスとは、それを**実装** (implement) する型が持つべきフィールドの集合を指定する抽象型の一種だ。

例えば、Star Wars 三部作に出てくるキャラクターを表す `Character` というインターフェイスを以下のように定義できる。

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

これは、`Character` を**実装** (implement) する全ての型はそれらのフィールドを持つことを意味する。

例えば、`Character` を実装する型はこういうふうに書ける:

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

両方の型とも `Character` インターフェイス内の全てのフィールドを持っていることが分かる。
さらに、`totalCredits`、 `starships`、`primaryFunction` といったそれぞれのキャラクター型に特定なフィールドも追加している。

### メッセージ

フィールドの他に、インターフェイスはメッセージを宣言することもできる。

```
package com.example
@target(Scala)

## Starship represents the starships in Star Wars.
interface Starship {
  name: String!
  length(unit: com.example.LengthUnit): Double
}
```

これは `Starship` を実装する型は上記のフィールドとメッセージの両方を持っている必要があることを意味する。

### extra コード

生成コードに直接 Scala や Java のコードを埋め込むための非常手段として Contraband は特殊なコメント記法を提供する。

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

- `#x` は生成されるクラスの本文内にコードを挿入する。
- `#xinterface` は親クラスを追加する。
- `#xtostring` は `toString` メソッドのカスタム化に使用する。
- `#xcompanion` は、生成されるクラスのコンパニオンオブジェクト内にコードを挿入する。
- `#xcompanioninterface` はコンパニオンオブジェクトに親クラスを追加する。
