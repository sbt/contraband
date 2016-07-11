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

  def generate(s: Schema, i: Interface, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    // We generate the code that corresponds to this protocol, but without its children, because they
    // may target another language. In this case, we have to make sure that we won't generate them
    // in the wrong language.
    val childLessProtocol = i.copy(children = Nil)
    val parentResult = childLessProtocol.targetLang match {
      case "Scala" => scalaGen.generate(s, childLessProtocol, parent, superFields) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, childLessProtocol, parent, superFields) mapV (_ indentWith javaGen.indentationConfiguration)
    }

    (parentResult :: (i.children map (generate(s, _, Some(i), i.fields ++ superFields)))) reduce (_ merge _)

  }

  def generate(s: Schema, r: Record, parent: Option[Interface], superFields: List[Field]): ListMap[File, String] = {
    r.targetLang match {
      case "Scala" => scalaGen.generate(s, r, parent, superFields) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, r, parent, superFields) mapV (_ indentWith javaGen.indentationConfiguration)
    }
  }

  def generate(s: Schema, e: Enumeration): ListMap[File, String] =
    e.targetLang match {
      case "Scala" => scalaGen.generate(s, e) mapV (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, e) mapV (_ indentWith javaGen.indentationConfiguration)
    }
}
