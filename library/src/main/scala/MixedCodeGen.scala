package sbt.datatype

import java.io.File

/**
 * Generator that produces both Scala and Java code.
 */
class MixedCodeGen(javaLazy: String, genScalaFileName: Definition => File, scalaSealprotocols: Boolean) extends CodeGenerator {
  val javaGen  = new JavaCodeGen(javaLazy)
  val scalaGen = new ScalaCodeGen(genScalaFileName, scalaSealprotocols)

  def generate(s: Schema): Map[File, String] =
    s.definitions map (generate (s, _, None, Nil)) reduce (_ merge _)

  def generate(s: Schema, p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    // We generate the code that corresponds to this protocol, but without its children, because they
    // may target another language. In this case, we have to make sure that we won't generate them
    // in the wrong language.
    val childLessProtocol = p.copy(children = Nil)
    val parentResult = childLessProtocol.targetLang match {
      case "Scala" => scalaGen.generate(s, childLessProtocol, parent, superFields) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, childLessProtocol, parent, superFields) mapValues (_ indentWith javaGen.indentationConfiguration)
    }

    (parentResult :: (p.children map (generate(s, _, Some(p), p.fields ++ superFields)))) reduce (_ merge _)

  }

  def generate(s: Schema, r: Record, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    r.targetLang match {
      case "Scala" => scalaGen.generate(s, r, parent, superFields) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, r, parent, superFields) mapValues (_ indentWith javaGen.indentationConfiguration)
    }
  }

  def generate(s: Schema, e: Enumeration): Map[File, String] =
    e.targetLang match {
      case "Scala" => scalaGen.generate(s, e) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(s, e) mapValues (_ indentWith javaGen.indentationConfiguration)
    }
}
