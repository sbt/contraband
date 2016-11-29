package sbt.contraband
package ast

import org.parboiled2.Position
// import sangria.parser.SourceMapper
// import sangria.renderer.QueryRenderer

import scala.collection.immutable.ListMap

case class Document(
    packageDecl: Option[PackageDecl],
    definitions: List[Definition],
    directives: List[Directive],
    trailingComments: List[Comment] = Nil,
    position: Option[Position] = None //,
    // sourceMapper: Option[SourceMapper] = None
    ) extends AstNode with WithTrailingComments {
  // lazy val operations = Map(definitions collect {case op: OperationDefinition ⇒ op.name → op}: _*)
  // lazy val fragments = Map(definitions collect {case fragment: FragmentDefinition ⇒ fragment.name → fragment}: _*)
  // lazy val source = sourceMapper map (_.source)

  // def operationType(operationName: Option[String] = None): Option[OperationType] =
  //   operation(operationName) map (_.operationType)

  // def operation(operationName: Option[String] = None): Option[OperationDefinition] =
  //   if (operations.size != 1 && operationName.isEmpty)
  //     None
  //   else
  //     operationName flatMap (opName ⇒ operations get Some(opName)) orElse operations.values.headOption

  // def withoutSourceMapper = copy(sourceMapper = None)

  // override def canEqual(other: Any): Boolean = other.isInstanceOf[Document]

  // override def equals(other: Any): Boolean = other match {
  //   case that: Document ⇒
  //     (that canEqual this) &&
  //       definitions == that.definitions &&
  //       position == that.position
  //   case _ ⇒ false
  // }

  // /**
  //   * Merges two documents. The `sourceMapper` is lost along the way.
  //   */
  // def merge(other: Document) = Document.merge(Vector(this, other))

  // override def hashCode(): Int =
  //   Seq(definitions, position).map(_.hashCode()).foldLeft(0)((a, b) ⇒ 31 * a + b)
}

case class PackageDecl(nameSegments: List[String], directives: List[Directive] = Nil, comments: List[Comment] = Nil, position: Option[Position] = None)

sealed trait WithComments extends AstNode {
  def comments: List[Comment]
}

sealed trait WithTrailingComments {
  def trailingComments: List[Comment]
}

sealed trait Type extends AstNode {
  def name: String = namedType.names.mkString(".")

  def namedType: NamedType = {
    @annotation.tailrec
    def loop(tpe: Type): NamedType = tpe match {
      case NotNullType(ofType, _) => loop(ofType)
      case LazyType(ofType, _) => loop(ofType)
      case ListType(ofType, _) => loop(ofType)
      case named: NamedType => named
    }
    loop(this)
  }

  def isNotNullType: Boolean = {
    @annotation.tailrec
    def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(ofType, _) => true
        case LazyType(ofType, _) => loop(ofType)
        case ListType(ofType, _) => loop(ofType)
        case named: NamedType => false
      }
    loop(this)
  }

  def isListType: Boolean = {
    @annotation.tailrec
    def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(ofType, _) => loop(ofType)
        case LazyType(ofType, _) => loop(ofType)
        case ListType(ofType, _) => true
        case named: NamedType => false
      }
    loop(this)
  }

  def isOptionalType: Boolean =
    !isListType && !isNotNullType

  def isLazyType: Boolean = {
    @annotation.tailrec
    def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(ofType, _) => loop(ofType)
        case LazyType(ofType, _) => true
        case ListType(ofType, _) => loop(ofType)
        case named: NamedType => false
      }
    loop(this)
  }


  /** Removes all type parameters from `tpe` */
  def removeTypeParameters: ast.Type =
    {
      /** Removes all type parameters from `tpe` */
      def removeTp(tpe: String): String = tpe.replaceAll("<.+>", "").replaceAll("\\[.+\\]", "")
      def loop(tpe: Type): Type =
        tpe match {
          case NotNullType(ofType, pos) => NotNullType(loop(ofType), pos)
          case LazyType(ofType, pos)    => LazyType(loop(ofType), pos)
          case ListType(ofType, pos)    => ListType(loop(ofType), pos)
          case named: NamedType => NamedType(
            removeTp(named.name) match {
              case s if s contains "." => s.split('.').toList
              case s                   => s :: Nil
            }, named.position)
        }
      loop(this)
    }

  def notNull: ast.Type =
    {
      def loop(tpe: Type): Type =
        tpe match {
          case nn: NotNullType       => nn
          case lt: ListType          => lt
          case LazyType(ofType, pos) => LazyType(loop(ofType), pos)
          case named: NamedType      => NotNullType(named, None)
        }
      loop(this)
    }
}

case class NamedType(names: List[String], position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[NamedType]

  override def equals(other: Any): Boolean =
    other match {
      case that: NamedType =>
        (that canEqual this) &&
          names == that.names
      case _ => false
    }

  override def hashCode: Int = {
    37 * (17 + names.##) + "NamedType".##
  }
}
object NamedType {
  def apply(name: String, position: Option[Position]): NamedType =
    NamedType(if (name contains ".") name.split('.').toList
              else name :: Nil, position)
}
case class NotNullType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[NotNullType]

  override def equals(other: Any): Boolean =
    other match {
      case that: NotNullType =>
        (that canEqual this) &&
          ofType == that.ofType
      case _ => false
    }

  override def hashCode: Int = {
    37 * (17 + ofType.##) + "NotNullType".##
  }
}
case class ListType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[ListType]

  override def equals(other: Any): Boolean =
    other match {
      case that: ListType =>
        (that canEqual this) &&
          ofType == that.ofType
      case _ => false
    }

  override def hashCode: Int = {
    37 * (17 + ofType.##) + "ListType".##
  }
}
case class LazyType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[LazyType]

  override def equals(other: Any): Boolean =
    other match {
      case that: LazyType =>
        (that canEqual this) &&
          ofType == that.ofType
      case _ => false
    }

  override def hashCode: Int = {
    37 * (17 + ofType.##) + "LazyType".##
  }
}

sealed trait NameValue extends AstNode with WithComments {
  def name: String
  def value: Value
}

case class Directive(name: String, arguments: List[Argument], comments: List[Comment] = Nil, position: Option[Position] = None) extends AstNode

object Directive {
  private val java = EnumValue("Java")
  private val scala = EnumValue("Scala")

  val targetJava: Directive = Directive("target", Argument(None, java) :: Nil)
  val targetScala: Directive = Directive("target", Argument(None, scala) :: Nil)
  def since(value: String): Directive = Directive("since", Argument(None, StringValue(value)) :: Nil)
  def codecPackage(value: String): Directive = Directive("codecPackage", Argument(None, StringValue(value)) :: Nil)
  def fullCodec(value: String): Directive = Directive("fullCodec", Argument(None, StringValue(value)) :: Nil)
  def codecTypeField(value: String): Directive = Directive("codecTypeField", Argument(None, StringValue(value)) :: Nil)
}

case class Argument(nameOpt: Option[String], value: Value, comments: List[Comment] = Nil, position: Option[Position] = None) extends AstNode with WithComments

sealed trait Value extends AstNode with WithComments {
  def renderPretty: String
  // override def renderPretty: String = QueryRenderer.render(this, QueryRenderer.PrettyInput)
}

sealed trait ScalarValue extends Value

case class IntValue(value: Int, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class BigIntValue(value: BigInt, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class FloatValue(value: Double, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class BigDecimalValue(value: BigDecimal, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class StringValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class BooleanValue(value: Boolean, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue {
  def renderPretty: String = value.toString
}
case class EnumValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = value.toString
}
case class ListValue(values: List[Value], comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = values.toString
}
case class VariableValue(name: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = name
}
case class NullValue(comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = "null"
}
case class ObjectValue(fields: List[ObjectField], comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = "{}"
  lazy val fieldsByName =
    fields.foldLeft(ListMap.empty[String, Value]) {
      case (acc, field) ⇒ acc + (field.name → field.value)
    }
}

case class ObjectField(name: String, value: Value, comments: List[Comment] = Nil, position: Option[Position] = None) extends NameValue {
  def renderPretty: String = s"$name"
}
case class RawValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  def renderPretty: String = value
}

sealed trait Comment {
  def text: String
}
case class CommentLine(text: String, position: Option[Position] = None) extends Comment
case class DocComment(text: String, position: Option[Position] = None) extends Comment
case class ExtraComment(text: String, position: Option[Position] = None) extends Comment
case class ExtraIntfComment(text: String, position: Option[Position] = None) extends Comment
case class ToStringImplComment(text: String, position: Option[Position] = None) extends Comment
case class CompanionExtraIntfComment(text: String, position: Option[Position] = None) extends Comment
case class CompanionExtraComment(text: String, position: Option[Position] = None) extends Comment

// Schema definitions

case class FieldDefinition(
  name: String,
  fieldType: Type,
  arguments: List[InputValueDefinition],
  defaultValue: Option[Value] = None,
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  position: Option[Position] = None) extends SchemaAstNode {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[FieldDefinition]

  override def equals(other: Any): Boolean = other match {
    case that: FieldDefinition =>
      (that canEqual this) &&
        name == that.name &&
        fieldType == that.fieldType &&
        arguments == that.arguments
    case _ => false
  }

  override def hashCode: Int = {
    37 * (37 * (37 * (17 + name.##) + fieldType.##) + arguments.##)
  }
}

case class InputValueDefinition(
  name: String,
  valueType: Type,
  defaultValue: Option[Value],
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  position: Option[Position] = None) extends SchemaAstNode {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[InputValueDefinition]

  override def equals(other: Any): Boolean = other match {
    case that: InputValueDefinition =>
      (that canEqual this) &&
        name == that.name &&
        valueType == that.valueType
    case _ => false
  }

  override def hashCode: Int = {
    37 * (17 + name.##) + valueType.##
  }
}

case class ObjectTypeDefinition(
  name: String,
  namespace: Option[String],
  interfaces: List[NamedType],
  fields: List[FieldDefinition],
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  trailingComments: List[Comment] = Nil,
  position: Option[Position] = None) extends RecordLikeDefinition with WithTrailingComments

case class InterfaceTypeDefinition(
  name: String,
  namespace: Option[String],
  interfaces: List[NamedType],
  fields: List[FieldDefinition],
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  trailingComments: List[Comment] = Nil,
  position: Option[Position] = None) extends RecordLikeDefinition with WithTrailingComments

case class EnumTypeDefinition(
  name: String,
  namespace: Option[String],
  values: List[EnumValueDefinition],
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  trailingComments: List[Comment] = Nil,
  position: Option[Position] = None) extends TypeDefinition with WithTrailingComments

case class EnumValueDefinition(
  name: String,
  directives: List[Directive] = Nil,
  comments: List[Comment] = Nil,
  position: Option[Position] = None) extends SchemaAstNode

sealed trait SchemaAstNode extends AstNode with WithComments
sealed trait TypeSystemDefinition extends SchemaAstNode with Definition
sealed trait RecordLikeDefinition extends TypeDefinition {
  def fields: List[FieldDefinition]
}
sealed trait TypeDefinition extends TypeSystemDefinition {
  def name: String
  def namespace: Option[String]
  def directives: List[Directive]
  def comments: List[Comment]
  def trailingComments: List[Comment]
}
sealed trait Definition extends AstNode

sealed trait AstNode {

}

object AstUtil {
  def toDoc(comments: List[Comment]): List[String] =
    comments collect {
      case DocComment(text, _) => text.trim
    }

  def toExtra(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect {
      case ExtraComment(text, _) => text
    }

  def toExtraIntf(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect {
      case ExtraIntfComment(text, _) => text
    }

  def toToStringImpl(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect {
      case ToStringImplComment(text, _) => text
    }

  def toCompanionExtraIntfComment(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect {
      case CompanionExtraIntfComment(text, _) => text
    }

  def toCompanionExtra(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect {
      case CompanionExtraComment(text, _) => text
    }

  def getTarget(opt: Option[String]): String =
    opt match {
      case Some(x) => x
      case _       => sys.error("@target directive must be set either at the defnition or at the package.")
    }

  def scanSingleStringDirective(dirs: List[Directive], name: String): Option[String] =
    scanSingleDirective(dirs, name) map { d =>
      val args = d.arguments
      args.size match {
        case x if x != 1 => sys.error(s"One argument is expected for @$name!")
        case 1 =>
          args.head.value match {
            case StringValue(value, _, _) => value
            case v => sys.error(s"Unexpected value for @$name: $v")
          }
      }
    }

  def scanSingleDirective(dirs: List[Directive], name: String): Option[Directive] =
    {
      val ts = dirs collect { case d@Directive(n, _, _, _) if n == name => d }
      ts.size match {
        case 0 => None
        case x if x > 1 => sys.error(s"More than one @$name directive was found!")
        case 1 => ts.headOption
      }
    }

  def toTarget(dirs: List[Directive]): Option[String] =
    scanSingleDirective(dirs, "target") map { d =>
      val args = d.arguments
      args.size match {
        case x if x != 1 => sys.error("One argument is expected for @target!")
        case 1 =>
          args.head.value match {
            case EnumValue(value, _, _) => value
            case v => sys.error(s"Unexpected value for @target: $v")
          }
      }
    }

  def toSince(dirs: List[Directive]): Option[VersionNumber] =
    scanSingleStringDirective(dirs, "since") map { s =>
      VersionNumber(s)
    }

  def getSince(dirs: List[Directive]): VersionNumber =
    toSince(dirs) match {
      case Some(x) => x
      case _       => VersionNumber.empty
    }

  def toCodecPackage(d: Document): Option[String] =
    {
      val dirs = d.directives ++ (d.packageDecl map {_.directives}).toList.flatten
      scanSingleStringDirective(dirs, "codecPackage") orElse
      scanSingleStringDirective(dirs, "codecNamespace")
    }


  def toFullCodec(d: Document): Option[String] =
    {
      val dirs = d.directives ++ (d.packageDecl map {_.directives}).toList.flatten
      scanSingleStringDirective(dirs, "fullCodec")
    }

  def toCodecTypeField(d: Document): Option[String] =
    toCodecTypeField(d.directives ++ (d.packageDecl map {_.directives}).toList.flatten)

  def toCodecTypeField(dirs: List[Directive]): Option[String] =
    scanSingleStringDirective(dirs, "codecTypeField")

  def toNamedType(i: InterfaceTypeDefinition, pkg: Option[String]): NamedType =
    {
      val ns =
        (i.namespace orElse pkg) match {
          case Some(x) => x.split('.').toList
          case _       => Nil
        }
      NamedType(ns ++ i.name.split('.').toList, None)
    }
}
