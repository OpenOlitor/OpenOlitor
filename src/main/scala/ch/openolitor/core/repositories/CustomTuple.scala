package ch.openolitor.core.repositories

import scala.runtime.ScalaRunTime

trait CustomTuple {
  this: Product =>

  override def toString() = "(" + this.productIterator.mkString(", ") + ")"

  override def hashCode(): Int = ScalaRunTime._hashCode(this)

  override def equals(other: Any): Boolean

}