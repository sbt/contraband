package sbt.datatype

trait Zero[A] {
  def zero: A
}

object Zero {
  def apply[A](z: => A): Zero[A] = new Zero[A] {
    def zero: A = z
  }
  implicit val stringZero: Zero[String] = Zero[String]("")
  implicit val intZero: Zero[Int] = Zero[Int](0)
  implicit val longZero: Zero[Long] = Zero[Long](0)
}
