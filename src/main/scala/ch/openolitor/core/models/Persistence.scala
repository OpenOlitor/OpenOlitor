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

import spray.json.JsValue
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.domain.PersistentEvent

case class PersistedMessage(persistenceId: String, sequenceNr: Long, payload: JsValue) extends JSONSerializable

case class PersistenceQueryParams(key: Option[String], from: Option[Long], to: Option[Long], content: Option[String]) extends JSONSerializable

case class PersistenceJournal(
  persistenceKey: Long,
  sequenceNr: Long,
  message: Option[PersistedMessage]
) extends JSONSerializable

case class PersistenceMetadata(
  persistenceId: String,
  persistenceKey: Long,
  sequenceNr: Long
) extends JSONSerializable

case class PersistenceMessage(
  persistenceId: String,
  sequenceNr: Long,
  message: Option[PersistentEvent]
) extends JSONSerializable