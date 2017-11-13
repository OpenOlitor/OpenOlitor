/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.repositories

import scalikejdbc._
import ch.openolitor.core.models.BaseId
import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.core.models.PersonId
import scala.collection.immutable.TreeMap
import ch.openolitor.core.models.PersonId
import java.util.Locale
import ch.openolitor.core.models.BaseStringId

trait DBMappings extends BaseParameter
    with Parameters
    with Parameters23
    with Parameters24
    with Parameters25
    with Parameters26
    with Parameters27
    with Parameters28
    with LowPriorityImplicitsParameterBinderFactory1 {
  import Binders._
  import ParameterBinderFactory._

  def baseIdBinders[T <: BaseId](f: Long => T): Binders[T] = Binders.long.xmap(l => f(l), _.id)
  def baseStringIdBinders[T <: BaseStringId](f: String => T): Binders[T] = Binders.string.xmap(l => f(l), _.id)
  def optionBaseIdBinders[T <: BaseId](f: Long => T): Binders[Option[T]] = Binders.optionLong.xmap(_.map(l => f(l)), _.map(_.id))
  def toStringBinder[V](f: String => V): Binders[V] = Binders.string.xmap(f(_), _.toString)
  def seqSqlBinder[V](f: String => V, g: V => String): Binders[Seq[V]] = Binders.string.xmap({
    Option(_) match {
      case None => Seq() // TODO change all the seq fields to not null default ""
      case Some(x) =>
        if (x.isEmpty) {
          Seq()
        } else {
          x.split(",") map (f) toSeq
        }
    }
  }, { values => values map (g) mkString (",") })
  def setSqlBinder[V](f: String => V, g: V => String): Binders[Set[V]] = Binders.string.xmap({
    Option(_) match {
      case None => Set() // TODO change all the seq fields to not null default ""
      case Some(x) =>
        if (x.isEmpty) {
          Set()
        } else {
          x.split(",") map (f) toSet
        }
    }
  }, { values => values map (g) mkString (",") })
  def seqBaseIdBinders[T <: BaseId](f: Long => T): Binders[Seq[T]] = seqSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseIdBinders[T <: BaseId](f: Long => T): Binders[Set[T]] = setSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseStringIdBinders[T <: BaseStringId](f: String => T): Binders[Set[T]] = setSqlBinder[T](l => f(l), _.id)
  def treeMapBinders[K: Ordering, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[TreeMap[K, V]] =
    Binders.string.xmap({
      Option(_) match {
        case None => TreeMap.empty[K, V]
        case Some(s) =>
          (TreeMap.empty[K, V] /: s.split(",")) { (tree, str) =>
            str.split("=") match {
              case Array(left, right) =>
                tree + (kf(left) -> vf(right))
              case _ =>
                tree
            }
          }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })
  def mapBinders[K, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[Map[K, V]] =
    Binders.string.xmap({
      Option(_) match {
        case None => Map.empty[K, V]
        case Some(s) =>
          (Map.empty[K, V] /: s.split(",")) { (tree, str) =>
            str.split("=") match {
              case Array(left, right) =>
                tree + (kf(left) -> vf(right))
              case _ =>
                tree
            }
          }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })

  implicit val localeBinder: Binders[Locale] = Binders.string.xmap(l => Locale.forLanguageTag(l), _.toLanguageTag)
  implicit val personIdBinder: Binders[PersonId] = baseIdBinders(PersonId.apply _)

  implicit val charArrayBinder: Binders[Array[Char]] = Binders.string.xmap(_.toCharArray, x => new String(x))
  implicit val optionCharArrayBinder: Binders[Option[Array[Char]]] = Binders.string.xmap(s => Option(s).map(_.toCharArray), _.map(x => new String(x)).getOrElse(null))

  implicit val stringSeqBinders: Binders[Seq[String]] = seqSqlBinder(identity, identity)
  implicit val stringSetBinders: Binders[Set[String]] = setSqlBinder(identity, identity)

  // low level binders
  import TypeBinder._
  import ParameterBinderFactory._
  implicit val stringBinder = Binders.string
  implicit val optionStringBinder = Binders.option[String]
  implicit val intBinder = Binders.int
  implicit val optionIntBinder = Binders.optionInt
  implicit val longBinder = Binders.long
  implicit val optionLongBinder = Binders.optionLong
  implicit val shortBinder = Binders.short
  implicit val optionShortBinder = Binders.optionShort
  implicit val floatBinder = Binders.float
  implicit val optionFloatBinder = Binders.optionFloat
  implicit val doubleBinder = Binders.double
  implicit val optionDoubleBinder = Binders.optionDouble
  implicit val booleanBinder = Binders.boolean
  implicit val optionBooleanBinder = Binders.optionBoolean
  implicit val localDateBinder = Binders.jodaLocalDate
  implicit val optionLocalDateBinder = Binders.option[LocalDate]
  implicit val datetimeBinder = Binders.jodaDateTime
  implicit val optionDatetimeBinder = Binders.option[DateTime]
  implicit val bigDecimalBinder = Binders.bigDecimal
  implicit val optionBigDecimalBinder = Binders.option[BigDecimal]

  // low level parameterbinderfactories

}
