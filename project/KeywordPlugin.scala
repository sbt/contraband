import sbt._
import Keys._

object KeywordPlugin extends AutoPlugin {
  override val requires = plugins.JvmPlugin

  lazy val scalaKeywords = TaskKey[Set[String]]("scala-keywords")
  lazy val generateKeywords = TaskKey[File]("generateKeywords")

  def getScalaKeywords: Set[String] = {
    val g = new scala.tools.nsc.Global(new scala.tools.nsc.Settings)
    g.nme.keywords.map(_.toString)
  }
  def writeScalaKeywords(base: File, keywords: Set[String]): File = {
    val init = keywords.toList.sortBy(identity).map(tn => '"' + tn + '"').mkString("Set(", ", ", ")")
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
  override def projectSettings: Seq[Setting[_]] = inConfig(Compile)(
    Seq(
      scalaKeywords := getScalaKeywords,
      generateKeywords := writeScalaKeywords(sourceManaged.value, scalaKeywords.value),
      sourceGenerators += Def.task(Seq(generateKeywords.value)).taskValue
    )
  )
}
