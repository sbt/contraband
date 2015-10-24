package sbt.datatype

import java.io.File

/**
 * Generator that produces both Scala and Java code.
 */
class MixedCodeGen(javaLazy: String, genScalaFileName: Definition => File, scalaSealprotocols: Boolean) extends CodeGenerator {
  val javaGen  = new JavaCodeGen(javaLazy)
  val scalaGen = new ScalaCodeGen(genScalaFileName, scalaSealprotocols)

  def generate(s: Schema): Map[File, String] =
    s.definitions map (generate (_, None, Nil)) reduce (_ merge _)

  def generate(p: Protocol, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    // We generate the code that corresponds to this protocol, but without its children, because they
    // may target another language. In this case, we have to make sure that we won't generate them
    // in the wrong language.
    val childLessProtocol = p.copy(children = Nil)
    val parentResult = childLessProtocol.targetLang match {
      case "Scala" => scalaGen.generate(childLessProtocol, parent, superFields) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(childLessProtocol, parent, superFields) mapValues (_ indentWith javaGen.indentationConfiguration)
    }

    (parentResult :: (p.children map (generate(_, Some(p), p.fields ++ superFields)))) reduce (_ merge _)

  }

  def generate(r: Record, parent: Option[Protocol], superFields: List[Field]): Map[File, String] = {
    r.targetLang match {
      case "Scala" => scalaGen.generate(r, parent, superFields) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(r, parent, superFields) mapValues (_ indentWith javaGen.indentationConfiguration)
    }
  }

  def generate(e: Enumeration): Map[File, String] =
    e.targetLang match {
      case "Scala" => scalaGen.generate(e) mapValues (_ indentWith scalaGen.indentationConfiguration)
      case "Java"  => javaGen.generate(e) mapValues (_ indentWith javaGen.indentationConfiguration)
    }
}
