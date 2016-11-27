package sbt.contraband

import scala.compat.Platform.EOL

/**
 * Implementation of a string buffer which takes care of indentation (according to `augmentIndentTrigger`,
 * `augmentIndentAfterTrigger`, `reduceIndentTrigger` and `reduceIndentAfterTrigger`) as text is added.
 */
class IndentationAwareBuffer(val config: IndentationConfiguration, private var level: Int = 0, private var inJavadoc: Boolean = false) {
  private val buffer: StringBuilder = new StringBuilder

  /** Add all the lines of `it` to the buffer. */
  def +=(it: Iterator[String]): Unit = it foreach append
  /** Add `s` to the buffer */
  def +=(s: String): Unit = s.lines foreach append

  override def toString: String = buffer.mkString

  private def append(s: String): Unit = {
    val clean = s.trim
    if (config.augmentIndentTrigger(clean)) level += 1
    if (config.reduceIndentTrigger(clean)) level = 0 max (level - 1)
    buffer append (config.indentElement * level + (if (inJavadoc) s else clean) + EOL)
    if (config.exitMultilineJavadoc(clean)) inJavadoc = false
    if (config.enterMultilineJavadoc(clean)) inJavadoc = true
    if (config.augmentIndentAfterTrigger(clean)) level += 1
    if (config.reduceIndentAfterTrigger(clean)) level = 0 max (level - 1)
  }
}

abstract class IndentationConfiguration {
  /** When this predicate holds for `s`, this line and the following should have one more level of indentation. */
  def augmentIndentTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, next lines should have one more level of indentation. */
  def augmentIndentAfterTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, this line and the following should have one less level of indentation. */
  def reduceIndentTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, next lines should have one less level of indentation. */
  def reduceIndentAfterTrigger(s: String): Boolean = false

  /** When this predicate holds for `s`, the next lines will be treated as multiline Javadoc. */
  def enterMultilineJavadoc(s: String): Boolean = false

  /** When this predicate holds for `s`, the next lines will no longer be treated as multiline Javadoc. */
  def exitMultilineJavadoc(s: String): Boolean = false

  /** Element of indentation. Prepended at each indented lines, as many times as there are indentation levels. */
  def indentElement: String = ""
}
