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

import akka.persistence.PersistentView
import akka.actor._
import scala.concurrent.duration._

/**
 * Diese generische EntityStoreView delelegiert die Events an die jeweilige modulspezifische ActorRef
 */
class EntityStoreView(insertActor: ActorRef, updateActor: ActorRef, deleteActor: ActorRef) extends PersistentView with ActorLogging {

  import EntityStore._

  override val persistenceId = EntityStore.persistenceId
  override val viewId = "stammdaten-entity-store"

  override def autoUpdateInterval = 100 millis

  /**
   * Delegate to
   */
  val receive: Receive = {
    case e: EntityStoreInitialized =>
      deleteActor ! e
    case e: EntityInsertedEvent =>
      insertActor ! e
    case e: EntityUpdatedEvent =>
      updateActor ! e
    case e: EntityDeletedEvent =>
      deleteActor ! e
  }
}