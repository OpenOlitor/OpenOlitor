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
package ch.openolitor.core.domain

import scala.util.Try
import ch.openolitor.core.models._
import scala.reflect._
import scala.reflect.runtime.universe.{ Try => TTry, _ }
import com.typesafe.scalalogging.LazyLogging
import scala.util.Success

/**
 * Used for handling custom module specific commands.
 * Validates the preconditions for a given command and returns resulting events.
 */
trait CommandHandler extends LazyLogging {
  import EntityStore._
  type IdFactory = Class[_ <: BaseId] => Long
  val handle: PartialFunction[UserCommand, IdFactory => EventMetadata => Try[Seq[PersistentEvent]]]

  def handleEntityInsert[E <: AnyRef, I <: BaseId: ClassTag](idFactory: IdFactory, meta: EventMetadata, entity: E, f: Long => I): Try[Seq[PersistentEvent]] = {
    val clOf = classTag[I].runtimeClass.asInstanceOf[Class[I]]
    logger.debug(s"created => Insert entity:$entity")
    val id = f(idFactory(clOf))
    Success(Seq(EntityInsertedEvent(meta, id, entity)))
  }
}