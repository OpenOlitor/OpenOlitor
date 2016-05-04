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
import akka.actor.SupervisorStrategy.Restart
import ch.openolitor._
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.domain._

trait EventService[E <: PersistentEvent] {
  type Handle = (E => Unit)
  val handle: Handle
}

/**
 * Component mit Referenzen auf weitere Dienste
 */
trait EntityStoreViewComponent extends Actor {
  import EntityStore._
  val insertService: EventService[EntityInsertedEvent[_, _]]
  val updateService: EventService[EntityUpdatedEvent[_, _]]
  val deleteService: EventService[EntityDeletedEvent[_]]

  val aktionenService: EventService[PersistentEvent]

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
    case _: Exception => Restart
  }
}

object EntityStoreView {
  case object Startup
}

/**
 * Diese generische EntityStoreView delelegiert die Events an die jeweilige modulspezifische ActorRef
 */
trait EntityStoreView extends PersistentView with ActorLogging {
  self: EntityStoreViewComponent =>

  import EntityStore._
  import EntityStoreView._

  val module: String

  override val persistenceId = EntityStore.persistenceId
  override val viewId = s"$module-entity-store"

  override def autoUpdateInterval = 100 millis

  /**
   * Delegate to
   */
  val receive: Receive = {
    case e: EntityStoreInitialized =>
      log.debug(s"Received EntityStoreInitialized")
      initializeEntityStoreView()
    case e: EntityInsertedEvent[_, _] =>
      insertService.handle(e)
    case e: EntityUpdatedEvent[_, _] =>
      updateService.handle(e)
    case e: EntityDeletedEvent[_] =>
      deleteService.handle(e)
    case Startup =>
      log.debug("Received startup command")
    case e: PersistentEvent =>
      // handle custom events
      aktionenService.handle(e)
  }

  def initializeEntityStoreView(): Unit
}