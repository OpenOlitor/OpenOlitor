object GenerateParametersTrait {
  def apply(n: Int): String = {
    s"""
package ch.openolitor.core.repositories

import ch.openolitor.core.scalax._
import scalikejdbc._

trait Parameters extends ${(1 to n) map (i => s"Parameters$i") mkString (" with ")}"""
  }
}