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

import akka.actor._
import akka.persistence._
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId

/**
 * Dieser EntityStore speichert alle Events, welche zu Modifikationen am Datenmodell führen können je Mandant.
 */
object EntityStore {
  import AggregateRoot._

  val VERSION = 1

  val persistenceId = "entity-store"

  case class EventStoreState(seqNr: Long, lastId: Option[UUID]) extends State
  def props(): Props = Props(classOf[EntityStore])

  //base commands
  case class InsertEntityCommand(entity: Any) extends Command
  case class UpdateEntityCommand(id: BaseId, entity: Any) extends Command
  case class DeleteEntityCommand(id: BaseId) extends Command

  //events raised by this aggregateroot
  case class EntityStoreInitialized(meta: EventMetadata) extends PersistetEvent
  case class EntityInsertedEvent(meta: EventMetadata, id: UUID, entity: Any) extends PersistetEvent
  case class EntityUpdatedEvent(meta: EventMetadata, id: BaseId, entity: Any) extends PersistetEvent
  case class EntityDeletedEvent(meta: EventMetadata, id: BaseId) extends PersistetEvent

  // other actor messages
}

class EntityStore extends AggregateRoot {
  import EntityStore._
  import AggregateRoot._

  log.debug(s"EntityStore: created")

  override def persistenceId: String = EntityStore.persistenceId

  type S = EventStoreState
  override var state: EventStoreState = EventStoreState(0, None)

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: PersistetEvent): Unit = {
    log.debug(s"updateState:$evt")
    evt match {
      case EntityStoreInitialized(_) =>
        context become created
      case _ =>
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    log.debug(s"restoreFromSnapshot:$state")
    state match {
      case Removed => context become removed
      case Created => context become created
      case s: EventStoreState => this.state = s
    }
  }

  /**
   * Status uninitialized
   */
  val uninitialized: Receive = {
    case GetState =>
      sender ! state
    case Initialize(state) =>
      //this event is used to initialize actor from within testcases
      log.error(s"Initialize: $state")
      this.state = state
      context become created
    case e =>
      log.error(s"Initialize eventstore with event:$e")
      state = incState
      persist(EntityStoreInitialized(metadata))(afterEventPersisted)
      context become created
      //reprocess event
      created(e)
  }

  /**
   * Eventlog initialized, handle entity events
   */
  val created: Receive = {
    case InsertEntityCommand(entity) =>
      log.debug(s"Insert entity:$entity")
      val event = EntityInsertedEvent(metadata, newId, entity)
      state = state.copy(seqNr = state.seqNr + 1, lastId = Some(event.id))
      persist(event)(afterEventPersisted)
      sender ! event
    case UpdateEntityCommand(id, entity) =>
      log.debug(s"Update entity::$id, $entity")
      state = incState
      persist(EntityUpdatedEvent(metadata, id, entity))(afterEventPersisted)
    case DeleteEntityCommand(entity) =>
      state = incState
      persist(EntityDeletedEvent(metadata, entity))(afterEventPersisted)
    case KillAggregate =>
      context.stop(self)
    case GetState =>
      sender ! state
    case other =>
      log.warning(s"Received unknown command:$other")
  }

  def metadata = {
    EventMetadata(VERSION, now, state.seqNr, persistenceId)
  }

  def incState = {
    state.copy(seqNr = state.seqNr + 1)
  }

  /**
   * PersistentActor was destroyed
   */
  val removed: Receive = {
    case GetState =>
      log.warning(s"Received GetState in state removed")
      sender() ! state
    case KillAggregate =>
      log.warning(s"Received KillAggregate in state removed")
      context.stop(self)
  }

  override val receiveCommand = uninitialized
}
