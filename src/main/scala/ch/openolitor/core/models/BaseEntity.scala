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
package ch.openolitor.core.models

import java.util.UUID
import scalikejdbc.ParameterBinder
import ch.openolitor.core.Macros
import spray.json.DefaultJsonProtocol
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.ws.ClientMessages.ClientMessage

trait BaseId extends AnyRef {
  val id: Long
}

trait BaseStringId extends AnyRef with JSONSerializable {
  val id: String
}

case class PersonId(id: Long) extends BaseId

trait PersonReference {
  val personId: PersonId
}

trait BaseEntity[T <: BaseId] extends Product with JSONSerializable {
  val id: T
  //modification flags on all entities
  val erstelldat: DateTime
  val ersteller: PersonId
  val modifidat: DateTime
  val modifikator: PersonId
}

sealed trait DBEvent[E <: Product] extends ClientMessage {
  val originator: PersonId
  val entity: E
}
sealed trait CRUDEvent[E <: BaseEntity[_ <: BaseId]] extends DBEvent[E] {
}
//events raised by this aggregateroot
case class EntityCreated[E <: BaseEntity[_ <: BaseId]](originator: PersonId, entity: E) extends CRUDEvent[E]
case class EntityModified[E <: BaseEntity[_ <: BaseId]](originator: PersonId, entity: E, orig: E) extends CRUDEvent[E]
case class EntityDeleted[E <: BaseEntity[_ <: BaseId]](originator: PersonId, entity: E) extends CRUDEvent[E]

case class DataEvent[E <: Product](originator: PersonId, entity: E) extends DBEvent[E]