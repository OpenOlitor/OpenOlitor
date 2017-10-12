object GenerateTuples {
  def apply(n: Int): String = {
    val types = (1 to n) map (i => s"T$i") mkString (",")
    val plusTypes = (1 to n) map (i => s"+T$i") mkString (",")
    
    s"""
package ch.openolitor.core.scalax

object Product$n {
  def unapply[$types](x: Product$n[$types]): Option[Product$n[$types]] =
    Some(x)
}

trait Product$n[$plusTypes] extends Product {

  override def productArity = $n

  @throws(classOf[IndexOutOfBoundsException])
  override def productElement(n: Int) = n match {
    ${(1 to n) map (i => s"case ${i-1} => _$i") mkString ("\n")}    
    case _ => throw new IndexOutOfBoundsException(n.toString())
  }

  ${(1 to n) map (i => s"def _$i: T$i") mkString ("\n")}  
}

case class Tuple$n[$plusTypes](${(1 to n) map (i => s"val _$i: T$i") mkString (",")}) extends Product$n[$types] with CustomTuple {
  override def canEqual(other: Any) = other.isInstanceOf[Tuple$n[${(1 to n) map (i => s"_") mkString (",")}]]
}
"""
  }
}