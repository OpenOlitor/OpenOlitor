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
package ch.openolitor.core

import spray.httpx.unmarshalling._
import ch.openolitor.core.models.BaseId
import spray.json._
import spray.routing._
import spray.httpx.unmarshalling._

trait SprayDeserializers {
  implicit val string2BooleanConverter = new Deserializer[String, Boolean] {
    def apply(value: String) = value.toLowerCase match {
      case "true" | "yes" | "on" => Right(true)
      case "false" | "no" | "off" => Right(false)
      case x => Left(MalformedContent("'" + x + "' is not a valid Boolean value"))
    }
  }

  def jsonDeserializer[T](implicit read: JsonReader[T]) = new Deserializer[String, T] {
    def apply(str: String) = {
      Right(read.read(str.parseJson))
    }
  }

  def long2BaseIdPathMatcher[T <: BaseId](implicit f: Long => T): spray.routing.PathMatcher1[T] = {
    PathMatchers.LongNumber.flatMap(id => Some(f(id)))
  }

  def enumPathMatcher[T](implicit f: String => Option[T]): spray.routing.PathMatcher1[T] = {
    PathMatchers.Segment.flatMap(id => f(id))
  }

  def long2BaseIdConverter[T <: BaseId](implicit f: Long => T) = new Deserializer[Long, T] {
    def apply(value: Long) = {
      try {
        Right(f(value))
      } catch {
        case e: Exception =>
          Left(MalformedContent(s"'$value' is not a valid id:$e"))
      }
    }

  }
}
