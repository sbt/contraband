package sbt.contraband
package ast

import org.parboiled2.Position

import scala.annotation.tailrec
import scala.collection.immutable.ListMap

import AstUtil.dotSep

final case class Document(
    packageDecl: Option[PackageDecl],
    definitions: List[Definition],
    directives: List[Directive],
    trailingComments: List[Comment] = Nil,
    position: Option[Position] = None
) extends AstNode
    with WithTrailingComments

final case class PackageDecl(
    nameSegments: List[String],
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    position: Option[Position] = None
)

sealed trait WithComments extends AstNode {
  def comments: List[Comment]
}

sealed trait WithTrailingComments {
  def trailingComments: List[Comment]
}

sealed trait Type extends AstNode {
  def name: String = namedType.names.mkString(".")

  def namedType: NamedType = {
    @tailrec def loop(tpe: Type): NamedType = tpe match {
      case NotNullType(ofType, _) => loop(ofType)
      case LazyType(ofType, _)    => loop(ofType)
      case ListType(ofType, _)    => loop(ofType)
      case named: NamedType       => named
    }
    loop(this)
  }

  def isNotNullType: Boolean = {
    @tailrec def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(_, _)   => true
        case LazyType(ofType, _) => loop(ofType)
        case ListType(ofType, _) => loop(ofType)
        case _: NamedType        => false
      }
    loop(this)
  }

  def isListType: Boolean = {
    @tailrec def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(ofType, _) => loop(ofType)
        case LazyType(ofType, _)    => loop(ofType)
        case ListType(_, _)         => true
        case _: NamedType           => false
      }
    loop(this)
  }

  def isOptionalType: Boolean =
    !isListType && !isNotNullType

  def isLazyType: Boolean = {
    @tailrec def loop(tpe: Type): Boolean =
      tpe match {
        case NotNullType(ofType, _) => loop(ofType)
        case LazyType(_, _)         => true
        case ListType(ofType, _)    => loop(ofType)
        case _: NamedType           => false
      }
    loop(this)
  }

  /** Removes all type parameters from `tpe` */
  def removeTypeParameters: ast.Type = {

    /** Removes all type parameters from `tpe` */
    def removeTp(tpe: String): String = tpe.replaceAll("<.+>", "").replaceAll("\\[.+\\]", "")
    def loop(tpe: Type): Type =
      tpe match {
        case NotNullType(ofType, pos) => NotNullType(loop(ofType), pos)
        case LazyType(ofType, pos)    => LazyType(loop(ofType), pos)
        case ListType(ofType, pos)    => ListType(loop(ofType), pos)
        case named: NamedType         => NamedType(dotSep(removeTp(named.name)), named.position)
      }
    loop(this)
  }

  def notNull: ast.Type = {
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

final case class NamedType(names: List[String], position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[NamedType]

  override def equals(other: Any): Boolean =
    other match {
      case that: NamedType => (that canEqual this) && names == that.names
      case _               => false
    }

  override def hashCode: Int = 37 * (17 + names.##) + "NamedType".##
}

object NamedType {
  def apply(name: String, position: Option[Position]): NamedType =
    NamedType(dotSep(name), position)
}

final case class NotNullType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[NotNullType]

  override def equals(other: Any): Boolean =
    other match {
      case that: NotNullType => (that canEqual this) && ofType == that.ofType
      case _                 => false
    }

  override def hashCode: Int = 37 * (17 + ofType.##) + "NotNullType".##
}

final case class ListType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[ListType]

  override def equals(other: Any): Boolean =
    other match {
      case that: ListType => (that canEqual this) && ofType == that.ofType
      case _              => false
    }

  override def hashCode: Int = 37 * (17 + ofType.##) + "ListType".##
}

final case class LazyType(ofType: Type, position: Option[Position] = None) extends Type {
  override def canEqual(other: Any): Boolean = other.isInstanceOf[LazyType]

  override def equals(other: Any): Boolean =
    other match {
      case that: LazyType => (that canEqual this) && ofType == that.ofType
      case _              => false
    }

  override def hashCode: Int = 37 * (17 + ofType.##) + "LazyType".##
}

sealed trait NameValue extends AstNode with WithComments {
  def name: String
  def value: Value
}

final case class Directive(
    name: String,
    arguments: List[Argument],
    comments: List[Comment] = Nil,
    position: Option[Position] = None
) extends AstNode

object Directive {
  private val java = EnumValue("Java")
  private val scala = EnumValue("Scala")

  val targetJava: Directive = Directive("target", Argument(None, java) :: Nil)
  val targetScala: Directive = Directive("target", Argument(None, scala) :: Nil)
  def since(value: String): Directive = Directive("since", Argument(None, StringValue(value)) :: Nil)
  def codecPackage(value: String): Directive = Directive("codecPackage", Argument(None, StringValue(value)) :: Nil)
  def fullCodec(value: String): Directive = Directive("fullCodec", Argument(None, StringValue(value)) :: Nil)
  def codecTypeField(value: String): Directive = Directive("codecTypeField", Argument(None, StringValue(value)) :: Nil)
  def generateCodec(value: Boolean): Directive = Directive("generateCodec", Argument(None, BooleanValue(value)) :: Nil)
  def modifier(value: String): Directive = Directive("modifier", Argument(None, StringValue(value)) :: Nil)
}

final case class Argument(
    nameOpt: Option[String],
    value: Value,
    comments: List[Comment] = Nil,
    position: Option[Position] = None
) extends AstNode
    with WithComments

sealed trait Value extends AstNode with WithComments {
  def renderPretty: String =
    this match {
      case x: IntValue        => x.value.toString
      case x: BigIntValue     => x.value.toString
      case x: FloatValue      => x.value.toString
      case x: BigDecimalValue => x.value.toString
      case x: StringValue     => "\"" + x.value.toString + "\""
      case x: BooleanValue    => x.value.toString
      case x: EnumValue       => x.value.toString
      case x: ListValue       => x.values.toString
      case x: VariableValue   => x.name
      case _: NullValue       => "null"
      case _: ObjectValue     => "{}"
      case x: RawValue        => x.value
    }
}

sealed trait ScalarValue extends Value

final case class IntValue(value: Int, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue
final case class BigIntValue(value: BigInt, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue
final case class FloatValue(value: Double, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue
final case class BigDecimalValue(value: BigDecimal, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue
final case class StringValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue
final case class BooleanValue(value: Boolean, comments: List[Comment] = Nil, position: Option[Position] = None) extends ScalarValue

final case class EnumValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value
final case class ListValue(values: List[Value], comments: List[Comment] = Nil, position: Option[Position] = None) extends Value
final case class VariableValue(name: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value
final case class NullValue(comments: List[Comment] = Nil, position: Option[Position] = None) extends Value
final case class RawValue(value: String, comments: List[Comment] = Nil, position: Option[Position] = None) extends Value

final case class ObjectValue(fields: List[ObjectField], comments: List[Comment] = Nil, position: Option[Position] = None) extends Value {
  lazy val fieldsByName: ListMap[String, Value] =
    fields.foldLeft(ListMap.empty[String, Value]) { case (acc, field) ⇒
      acc + (field.name → field.value)
    }
}

final case class ObjectField(name: String, value: Value, comments: List[Comment] = Nil, position: Option[Position] = None)
    extends NameValue {
  def renderPretty: String = s"$name"
}

sealed trait Comment {
  def text: String
}

final case class CommentLine(text: String, position: Option[Position] = None) extends Comment
final case class DocComment(text: String, position: Option[Position] = None) extends Comment
final case class ExtraComment(text: String, position: Option[Position] = None) extends Comment
final case class ExtraIntfComment(text: String, position: Option[Position] = None) extends Comment
final case class ToStringImplComment(text: String, position: Option[Position] = None) extends Comment
final case class CompanionExtraIntfComment(text: String, position: Option[Position] = None) extends Comment
final case class CompanionExtraComment(text: String, position: Option[Position] = None) extends Comment

// Schema definitions

final case class FieldDefinition(
    name: String,
    fieldType: Type,
    arguments: List[InputValueDefinition],
    defaultValue: Option[Value] = None,
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    position: Option[Position] = None
) extends SchemaAstNode {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[FieldDefinition]

  override def equals(other: Any): Boolean = other match {
    case that: FieldDefinition =>
      (that canEqual this) &&
      name == that.name &&
      fieldType == that.fieldType &&
      arguments == that.arguments
    case _ => false
  }

  override def hashCode: Int =
    37 * (37 * (37 * (17 + name.##) + fieldType.##) + arguments.##)
}

case class InputValueDefinition(
    name: String,
    valueType: Type,
    defaultValue: Option[Value],
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    position: Option[Position] = None
) extends SchemaAstNode {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[InputValueDefinition]

  override def equals(other: Any): Boolean = other match {
    case that: InputValueDefinition =>
      (that canEqual this) &&
      name == that.name &&
      valueType == that.valueType
    case _ => false
  }

  override def hashCode: Int = 37 * (17 + name.##) + valueType.##
}

final case class ObjectTypeDefinition(
    name: String,
    namespace: Option[String],
    interfaces: List[NamedType],
    fields: List[FieldDefinition],
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    trailingComments: List[Comment] = Nil,
    position: Option[Position] = None
) extends RecordLikeDefinition
    with WithTrailingComments

final case class InterfaceTypeDefinition(
    name: String,
    namespace: Option[String],
    interfaces: List[NamedType],
    fields: List[FieldDefinition],
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    trailingComments: List[Comment] = Nil,
    position: Option[Position] = None
) extends RecordLikeDefinition
    with WithTrailingComments

final case class EnumTypeDefinition(
    name: String,
    namespace: Option[String],
    values: List[EnumValueDefinition],
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    trailingComments: List[Comment] = Nil,
    position: Option[Position] = None
) extends TypeDefinition
    with WithTrailingComments

final case class EnumValueDefinition(
    name: String,
    directives: List[Directive] = Nil,
    comments: List[Comment] = Nil,
    position: Option[Position] = None
) extends SchemaAstNode

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

sealed trait AstNode

object AstUtil {
  def toDoc(comments: List[Comment]): List[String] =
    comments collect { case DocComment(text, _) =>
      text.trim
    }

  def toExtra(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect { case ExtraComment(text, _) =>
      text
    }

  def toExtraIntf(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect { case ExtraIntfComment(text, _) =>
      text
    }

  def toToStringImpl(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect { case ToStringImplComment(text, _) =>
      text
    }

  def toCompanionExtraIntfComment(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect { case CompanionExtraIntfComment(text, _) =>
      text
    }

  def toCompanionExtra(d: TypeDefinition): List[String] =
    (d.comments ++ d.trailingComments) collect { case CompanionExtraComment(text, _) =>
      text
    }

  def getTarget(opt: Option[String]): String =
    opt getOrElse sys.error("@target directive must be set either at the definition or at the package.")

  def scanSingleStringDirective(dirs: List[Directive], name: String): Option[String] =
    scanSingleDirectiveArgumentValue(dirs, name) { case StringValue(value, _, _) =>
      value
    }

  def scanSingleBooleanDirective(dirs: List[Directive], name: String): Option[Boolean] =
    scanSingleDirectiveArgumentValue(dirs, name) { case BooleanValue(value, _, _) =>
      value
    }

  def scanSingleDirective(dirs: List[Directive], name: String): Option[Directive] = {
    dirs filter (_.name == name) match {
      case Nil      => None
      case d :: Nil => Some(d)
      case _        => sys.error(s"More than one @$name directive was found!")
    }
  }

  def toTarget(dirs: List[Directive]): Option[String] =
    scanSingleDirectiveArgumentValue(dirs, "target") { case EnumValue(value, _, _) =>
      value
    }

  private def scanSingleDirectiveArgumentValue[A](dirs: List[Directive], name: String)(
      pf: PartialFunction[Value, A]
  ): Option[A] = {
    scanSingleDirective(dirs, name) map { d =>
      d.arguments match {
        case a :: Nil => applyOrElse(pf, a.value)(v => sys.error(s"Unexpected value for @$name: $v"))
        case _        => sys.error(s"One argument is expected for @target!")
      }
    }
  }

  def toModifier(dirs: List[Directive]): Option[String] =
    scanSingleStringDirective(dirs, "modifier")

  def toSince(dirs: List[Directive]): Option[VersionNumber] =
    scanSingleStringDirective(dirs, "since") map (VersionNumber(_))

  def getSince(dirs: List[Directive]): VersionNumber =
    toSince(dirs) getOrElse VersionNumber.empty

  def toCodecPackage(d: Document): Option[String] = {
    val dirs = d.directives ++ (d.packageDecl map { _.directives }).toList.flatten
    scanSingleStringDirective(dirs, "codecPackage") orElse
      scanSingleStringDirective(dirs, "codecNamespace")
  }

  def toFullCodec(d: Document): Option[String] = {
    val dirs = d.directives ++ (d.packageDecl map { _.directives }).toList.flatten
    scanSingleStringDirective(dirs, "fullCodec")
  }

  def toCodecTypeField(d: Document): Option[String] =
    toCodecTypeField(d.directives ++ (d.packageDecl map { _.directives }).toList.flatten)

  def toCodecTypeField(dirs: List[Directive]): Option[String] =
    scanSingleStringDirective(dirs, "codecTypeField")

  def toNamedType(i: InterfaceTypeDefinition, pkg: Option[String]): NamedType = {
    val ns =
      (i.namespace orElse pkg) match {
        case Some(x) => dotSep(x)
        case _       => Nil
      }
    NamedType(ns ++ dotSep(i.name), None)
  }

  def toGenerateCodec(dirs: List[Directive]): Option[Boolean] =
    scanSingleBooleanDirective(dirs, "generateCodec")

  def getGenerateCodec(dirs: List[Directive]): Boolean =
    toGenerateCodec(dirs).getOrElse(true)

  private[contraband] def dotSep(s: String): List[String] =
    if (s contains ".") s.split('.').toList else s :: Nil

  private def applyOrElse[A, B](pf: PartialFunction[A, B], x: A)(fb: A => B) = pf.applyOrElse(x, fb)
}
