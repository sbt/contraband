import sbt.*
import Keys.*

object KeywordPlugin extends AutoPlugin {
  override val requires = plugins.JvmPlugin

  lazy val scalaKeywords = TaskKey[Set[String]]("scala-keywords")
  @transient
  lazy val generateKeywords = TaskKey[File]("generateKeywords")

  private val scala3keywords = Seq("enum", "export", "given", "then")

  def getScalaKeywords: Set[String] = {
    val g = dotty.tools.dotc.core.StdNames
    g.nme.keywords.map(_.toString).toSet ++ scala3keywords
  }
  def writeScalaKeywords(base: File, keywords: Set[String]): File = {
    val init = keywords.toList.sortBy(identity).map(tn => "\"" + tn + "\"").mkString("Set(", ", ", ")")
    val objectName = "ScalaKeywords"
    val packageName = "sbt.contraband"
    val keywordsSrc =
      s"""package $packageName
           |object $objectName {
           |  val values = $init
           |}""".stripMargin
    val out = base / packageName.replace('.', '/') / (objectName + ".scala")
    IO.write(out, keywordsSrc)
    out
  }
  override def projectSettings: Seq[Setting[?]] = inConfig(Compile)(
    Seq(
      scalaKeywords := getScalaKeywords,
      generateKeywords := writeScalaKeywords(sourceManaged.value, scalaKeywords.value),
      sourceGenerators += Def.task(Seq(generateKeywords.value)).taskValue
    )
  )
}
