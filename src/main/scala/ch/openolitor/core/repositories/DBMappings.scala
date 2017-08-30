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
    with Parameters28 {
  import Binders._

  def baseIdBinders[T <: BaseId](f: Long => T): Binders[T] = Binders.long.xmap(l => f(l), _.id)
  def toStringBinder[V](f: String => V): Binders[V] = Binders.string.xmap(f(_), _.toString)
  def seqSqlBinder[V](f: String => V, g: V => String): Binders[Seq[V]] = Binders.string.xmap({ x => x.split(",") map (f) }, { values => values map (g) mkString (",") })
  def setSqlBinder[V](f: String => V, g: V => String): Binders[Set[V]] = Binders.string.xmap({ x => x.split(",") map (f) toSet }, { values => values map (g) mkString (",") })
  def seqBaseIdBinders[T <: BaseId](f: Long => T): Binders[Seq[T]] = seqSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseIdBinders[T <: BaseId](f: Long => T): Binders[Set[T]] = setSqlBinder[T](l => f(l.toLong), _.id.toString)
  def setBaseStringIdBinders[T <: BaseStringId](f: String => T): Binders[Set[T]] = setSqlBinder[T](l => f(l), _.id)
  def treeMapBinders[K: Ordering, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[TreeMap[K, V]] =
    Binders.string.xmap({ s =>
      (TreeMap.empty[K, V] /: s.split(",")) { (tree, str) =>
        str.split("=") match {
          case Array(left, right) =>
            tree + (kf(left) -> vf(right))
          case _ =>
            tree
        }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })
  def mapBinders[K, V](kf: String => K, vf: String => V, kg: K => String, vg: V => String): Binders[Map[K, V]] =
    Binders.string.xmap({ s =>
      (Map.empty[K, V] /: s.split(",")) { (tree, str) =>
        str.split("=") match {
          case Array(left, right) =>
            tree + (kf(left) -> vf(right))
          case _ =>
            tree
        }
      }
    }, { map =>
      map.toIterable.map { case (k, v) => kg(k) + "=" + vg(v) }.mkString(",")
    })


  implicit val localeBinder: Binders[Locale] = Binders.string.xmap(l => Locale.forLanguageTag(l), _.toLanguageTag)
  implicit val personIdBinder: Binders[PersonId] = baseIdBinders(PersonId.apply _)

  implicit val charArrayBinder: Binders[Array[Char]] = Binders.string.xmap(_.toCharArray, x => new String(x))

}
