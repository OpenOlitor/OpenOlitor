object GenerateParametersMapping {
  def apply(n: Int): String = {
    s"""
package ch.openolitor.core.repositories

import ch.openolitor.core.scalax._
import scalikejdbc._

trait Parameters$n extends BaseParameter {
  def parameters[${(1 to n) map (i => s"T$i") mkString (",")}](params: Tuple$n[${(1 to n) map (i => s"T$i") mkString (",")}])(
    implicit
    ${(1 to n) map{ i => 
      s"binder$i: Binders[T$i]"
    }  mkString (",")}
  ): Seq[ParameterBinder] = {
    Seq(
      ${(1 to n) map{ i => 
        s"binder$i(params._$i)"
      }  mkString (",")}
    )
  }
}
      """
  }
}