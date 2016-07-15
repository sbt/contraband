package sbt.datatype

import java.io.File
import scala.collection.immutable.ListMap

/**
 * Generator that produces both Scala and Java code.
 */
class MixedCodeGen(javaLazy: String, javaOptional: String, scalaArray: String, genScalaFileName: Definition => File, scalaSealprotocols: Boolean) extends CodeGenerator {
  val javaGen  = new JavaCodeGen(javaLazy, javaOptional)
  val scalaGen = new ScalaCodeGen(scalaArray, genScalaFileName, scalaSealprotocols)

  def generate(s: Schema): ListMap[File, String] =
    s.definitions map (generate (s, _, None, Nil)) reduce (_ merge _)

  def generateInterface(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    // We generate the code that corresponds to this protocol, but without its children, because they
    // may target another language. In this case, we have to make sure that we won't generate them
    // in the wrong language.
    val childLessProtocol = i.copy(children = Nil)
    val parentResult = childLessProtocol.targetLang match {
      case "Scala" => scalaGen.generateInterface(s, childLessProtocol, parent, superFields) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generateInterface(s, childLessProtocol, parent, superFields) mapV (_ indentWith javaGen.indentationConfiguration)
    }
    (parentResult :: (i.children map (generate(s, _, Some(i), superFields ++ i.fields)))) reduce (_ merge _)
  }

  def generateRecord(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    r.targetLang match {
      case "Scala" => scalaGen.generateRecord(s, r, parent, superFields) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generateRecord(s, r, parent, superFields) mapV (_ indentWith javaGen.indentationConfiguration)
    }
  }

  def generateEnum(s: Schema, e: Enumeration): ListMap[File, String] =
    e.targetLang match {
      case "Scala" => scalaGen.generateEnum(s, e) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generateEnum(s, e) mapV (_ indentWith javaGen.indentationConfiguration)
    }
}
