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
import ch.openolitor.core.models._
import ch.openolitor.core.Boot
import ch.openolitor.core.db.evolution.Evolution
import scala.util._
import ch.openolitor.core.db.ConnectionPoolContextAware
import scalikejdbc.DB
import ch.openolitor.core.SystemConfig
import spray.json.DefaultJsonProtocol
import ch.openolitor.core.BaseJsonProtocol
import org.joda.time.DateTime
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Macros._
import scala.reflect._
import scala.reflect.runtime.universe.{ Try => TTry, _ }
import ch.openolitor.buchhaltung.models._
import DefaultMessages._
import ch.openolitor.core.DBEvolutionReference

/**
 * _
 * Dieser EntityStore speichert alle Events, welche zu Modifikationen am Datenmodell führen können je Mandant.
 */
object EntityStore {
  import AggregateRoot._

  val VERSION = 2

  val persistenceId = "entity-store"

  case class EntityStoreState(dbSeeds: Map[Class[_ <: BaseId], Long]) extends State
  def props(dbEvolutionActor: ActorRef, evolution: Evolution)(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultEntityStore], sysConfig, dbEvolutionActor, evolution)

  //base commands
  case class InsertEntityCommand[E <: AnyRef](originator: PersonId, entity: E) extends UserCommand {
    val entityType = entity.getClass
  }
  case class UpdateEntityCommand[E <: AnyRef](originator: PersonId, id: BaseId, entity: E) extends UserCommand {
    val entityType = entity.getClass
  }
  case class DeleteEntityCommand(originator: PersonId, id: BaseId) extends UserCommand

  //events raised by this aggregateroot
  case class EntityStoreInitialized(meta: EventMetadata) extends PersistentEvent
  case class EntityInsertedEvent[I <: BaseId, E <: AnyRef](meta: EventMetadata, id: I, entity: E) extends PersistentEvent {
    val idType = id.getClass
  }
  case class EntityUpdatedEvent[I <: BaseId, E <: AnyRef](meta: EventMetadata, id: I, entity: E) extends PersistentEvent {
    val idType = id.getClass
  }
  case class EntityDeletedEvent[I <: BaseId](meta: EventMetadata, id: I) extends PersistentEvent

  trait ResultingEvent {
    def toPersistentEvent(implicit factory: EventMetadataFactory): PersistentEvent
  }
  case class EntityInsertEvent[I <: BaseId: ClassTag, E <: AnyRef](id: I, entity: E) extends ResultingEvent {
    def toPersistentEvent(implicit factory: EventMetadataFactory): PersistentEvent = EntityInsertedEvent(factory.newMetadata(), id, entity)
  }
  case class EntityUpdateEvent[I <: BaseId, E <: AnyRef](id: I, entity: E) extends ResultingEvent {
    def toPersistentEvent(implicit factory: EventMetadataFactory): PersistentEvent = EntityUpdatedEvent(factory.newMetadata(), id, entity)
  }
  case class EntityDeleteEvent[I <: BaseId](id: I) extends ResultingEvent {
    def toPersistentEvent(implicit factory: EventMetadataFactory): PersistentEvent = EntityDeletedEvent(factory.newMetadata(), id)
  }
  case class DefaultResultingEvent(eventF: EventMetadataFactory => PersistentEvent) extends ResultingEvent {
    def toPersistentEvent(implicit factory: EventMetadataFactory): PersistentEvent = eventF(factory)
  }

  case object StartSnapshotCommand

  // other actor messages  
  case object ReadSeedsFromDB

  case object UserCommandFailed
}

//json protocol
trait EntityStoreJsonProtocol extends BaseJsonProtocol {
  import EntityStore._

  implicit val metadataFormat = jsonFormat6(EventMetadata)
  implicit val eventStoreInitializedEventFormat = jsonFormat1(EntityStoreInitialized)
}

trait EntityStore extends AggregateRoot
    with ConnectionPoolContextAware
    with CommandHandlerComponent
    with DBEvolutionReference
    with IdFactory {

  import EntityStore._
  import AggregateRoot._

  val evolution: Evolution

  log.debug(s"EntityStore: created")

  override def dbInitialized(): Unit = {
    super.dbInitialized()
    readDBSeeds()
  }

  override def persistenceId: String = EntityStore.persistenceId

  type S = EntityStoreState
  override var state: EntityStoreState = EntityStoreState(Map())

  lazy val moduleCommandHandlers: List[CommandHandler] = List(
    stammdatenCommandHandler,
    buchhaltungCommandHandler,
    kundenportalCommandHandler,
    baseCommandHandler
  )

  def newId[I <: BaseId: ClassTag](cons: Long => I): I = {
    val clOf = classTag[I].runtimeClass.asInstanceOf[Class[I]]
    val id: Long = state.dbSeeds.get(clOf) map { id =>
      id + 1
    } getOrElse (sysConfig.mandantConfiguration.dbSeeds.get(clOf).getOrElse(1L))
    updateId(clOf, id)
    cons(id)
  }

  def updateId[E, I <: BaseId](clOf: Class[_ <: BaseId], id: Long) = {
    if (state.dbSeeds.get(clOf) map (_ < id) getOrElse (true)) {
      log.debug(s"updateId:$clOf -> $id")
      //only update if current id is smaller than new one or no id did exist 
      state = state.copy(dbSeeds = state.dbSeeds + (clOf -> id))
    }
  }

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(recovery: Boolean)(evt: PersistentEvent): Unit = {
    evt match {
      case EntityStoreInitialized(_) =>
        log.debug(s"EntityStoreInitialized")
      case e @ EntityInsertedEvent(meta, id, entity) =>
        updateId(e.idType, id.id)
        log.debug(s"EntityInsertedEvent, update id to:${e.idType} -> ${id.id}")
      case _ =>
    }
  }

  def readDBSeeds() = {
    implicit val personId = Boot.systemPersonId
    evolution.checkDBSeeds(Map()) match {
      case Success(newSeeds) =>
        log.debug(s"Read dbseeds:$newSeeds")
        state = state.copy(dbSeeds = newSeeds)
      case Failure(e) =>
        e.printStackTrace
        log.warning(s"Coulnd't read actual seeds from db {}", e)
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    log.debug(s"restoreFromSnapshot:$state")
    state match {
      case Removed => context become removed
      case Created => context become created
      case s: EntityStoreState => this.state = s
    }
  }

  /**
   * Status uninitialized
   */
  val uninitialized: Receive = {
    case GetState =>
      log.debug(s"uninitialized => GetState: $state")
      sender ! state
    case Initialize(state) =>
      //this event is used to initialize actor from within testcases
      log.debug(s"uninitialized => Initialize: $state")
      this.state = state
      context become created
    case Startup =>
      context become created
      sender ! Started
    case e =>
      log.debug(s"uninitialized => Initialize eventstore with event:$e, $self")
      persist(EntityStoreInitialized(metadata(Boot.systemPersonId).toMetadata(1L)))(afterEventPersisted)
      context become created
      //reprocess event
      created(e)
  }

  /**
   * Eventlog initialized, handle entity events
   */
  val created: Receive = {
    case KillAggregate =>
      log.debug(s"created => KillAggregate")
      context.stop(self)
    case GetState =>
      log.debug(s"created => GetState")
      sender ! state
    case command: UserCommand =>
      val meta = metadata(command.originator)
      val result = moduleCommandHandlers collectFirst { case ch: CommandHandler if ch.handle.isDefinedAt((command)) => ch.handle(command) } map { handle =>
        handle(this)(meta) match {
          case Success(resultingEvents) =>
            log.debug(s"handled command: $command in module specific command handler.")
            implicit val eventFactory = new EventMetadataFactory(meta)
            val result = resultingEvents map { resultingEvent =>
              val persistentEvent = resultingEvent.toPersistentEvent
              persist(persistentEvent)(afterEventPersisted)
              persistentEvent
            }
            //return only first event to sender
            result.headOption map { result =>
              sender ! result
            }
          case Failure(e) =>
            log.error(s"There was an error proccessing the command:$command, error:${e.getMessage}")
            sender ! UserCommandFailed
        }
      }
      if (result.isEmpty) {
        log.error(s"created => Received unknown command or no module handler handled the command:$command")
      }
    case StartSnapshotCommand =>
      //TODO: check if messages should also get deleted
      saveSnapshot(state)
      deleteMessages(lastSequenceNr)
    case DeleteMessagesSuccess(toSequenceNr) =>
    case DeleteMessagesFailure(error, toSequenceNr) =>
      log.error(s"Deleting of messages failed {}", error)
    case SaveSnapshotSuccess(metadata) =>
    case SaveSnapshotFailure(metadata, reason) =>
      log.error(s"SaveSnapshotFailure failed:$reason")
    case ReadSeedsFromDB =>
      readDBSeeds()
    case other =>
      log.warning(s"received unknown command:$other")
  }

  def metadata(personId: PersonId) = {
    EventTransactionMetadata(personId, VERSION, DateTime.now, aquireTransactionNr(), persistenceId)
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

class DefaultEntityStore(override val sysConfig: SystemConfig, override val dbEvolutionActor: ActorRef, override val evolution: Evolution) extends EntityStore
    with DefaultCommandHandlerComponent {
  val system = context.system
}
