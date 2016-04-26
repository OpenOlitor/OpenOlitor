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
import ch.openolitor.buchhaltung.models.RechnungModify
import ch.openolitor.buchhaltung.models.RechnungId

/**
 * _
 * Dieser EntityStore speichert alle Events, welche zu Modifikationen am Datenmodell führen können je Mandant.
 */
object EntityStore {
  import AggregateRoot._

  val VERSION = 1

  val persistenceId = "entity-store"

  case class EventStoreState(seqNr: Long, dbRevision: Int, dbSeeds: Map[Class[_ <: BaseId], BaseId]) extends State
  def props(evolution: Evolution)(implicit sysConfig: SystemConfig): Props = Props(classOf[EntityStore], sysConfig, evolution)

  //base commands
  case class InsertEntityCommand[E <: AnyRef](originator: UserId, entity: E) extends Command {
    val entityType = entity.getClass
  }
  case class UpdateEntityCommand[E <: AnyRef](originator: UserId, id: BaseId, entity: E) extends Command {
    val entityType = entity.getClass
  }
  case class DeleteEntityCommand(originator: UserId, id: BaseId) extends Command

  //events raised by this aggregateroot
  case class EntityStoreInitialized(meta: EventMetadata) extends PersistetEvent
  case class EntityInsertedEvent[I <: BaseId, E <: AnyRef](meta: EventMetadata, id: I, entity: E) extends PersistetEvent {
    val idType = id.getClass
  }
  case class EntityUpdatedEvent[I <: BaseId, E <: AnyRef](meta: EventMetadata, id: I, entity: E) extends PersistetEvent {
    val idType = id.getClass
  }
  case class EntityDeletedEvent[I <: BaseId](meta: EventMetadata, id: I) extends PersistetEvent

  // other actor messages
  case object CheckDBEvolution
}

//json protocol
trait EntityStoreJsonProtocol extends BaseJsonProtocol {
  import EntityStore._

  implicit val metadataFormat = jsonFormat5(EventMetadata)
  implicit val eventStoreInitializedEventFormat = jsonFormat1(EntityStoreInitialized)
}

class EntityStore(override val sysConfig: SystemConfig, evolution: Evolution) extends AggregateRoot with ConnectionPoolContextAware {
  import EntityStore._
  import AggregateRoot._

  log.debug(s"EntityStore: created")

  override def persistenceId: String = EntityStore.persistenceId

  type S = EventStoreState
  override var state: EventStoreState = EventStoreState(0, 0, Map())

  def newId[E, I <: BaseId](clOf: Class[_ <: BaseId])(implicit f: Long => I): I = {
    val id: Long = state.dbSeeds.get(clOf).map { id =>
      id.id + 1
    }.getOrElse(sysConfig.mandantConfiguration.dbSeeds.get(clOf).getOrElse(1L))
    log.debug(s"newId:$clOf -> $id")
    id
  }

  def updateId[E, I <: BaseId](clOf: Class[_ <: BaseId], id: I) = {
    log.debug(s"updateId:$clOf -> $id")
    if (state.dbSeeds.get(clOf).map(_.id < id.id).getOrElse(true)) {
      //only update if current id is smaller than new one or no id did exist 
      state = state.copy(dbSeeds = state.dbSeeds + (clOf -> id))
    }
  }

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: PersistetEvent): Unit = {
    log.debug(s"updateState:$evt")
    evt match {
      case EntityStoreInitialized(_)                 =>
      case e @ EntityInsertedEvent(meta, id, entity) => updateId(e.idType, id)
      case _                                         =>
    }
  }

  def checkDBEvolution(): Try[Int] = {
    log.debug(s"Check DB Evolution: current revision=${state.dbRevision}")
    implicit val userId = Boot.systemUserId
    evolution.evolveDatabase(state.dbRevision) match {
      case s @ Success(rev) =>
        log.debug(s"Successfully updated to db rev:$rev")
        updateDBRevision(rev)

        evolution.checkDBSeeds(state.dbSeeds) match {
          case Success(newSeeds) =>
            log.debug(s"Read dbseeds:$newSeeds")
            state = state.copy(dbSeeds = newSeeds)
          case Failure(e) =>
            log.warning(s"Coulnd't read actual seeds from db", e)
        }

        context become created
        s
      case f @ Failure(e) =>
        log.warning(s"dB Evolution failed", e)
        DB readOnly { implicit session =>
          val newRev = evolution.currentRevision
          updateDBRevision(newRev)
        }
        context become uncheckedDB
        f
    }
  }

  def updateDBRevision(newRev: Int) = {
    if (newRev != state.dbRevision) {
      state = state.copy(dbRevision = newRev)

      //save new snapshot
      saveSnapshot(state)
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    log.debug(s"restoreFromSnapshot:$state")
    state match {
      case Removed            => context become removed
      case Created            => context become created
      case s: EventStoreState => this.state = s
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
    case e =>
      log.debug(s"uninitialized => Initialize eventstore with event:$e, $self")
      state = incState
      persist(EntityStoreInitialized(metadata(Boot.systemUserId)))(afterEventPersisted)
      context become uncheckedDB
      //reprocess event
      uncheckedDB(e)
  }

  val uncheckedDB: Receive = {
    case CheckDBEvolution =>
      log.debug(s"uncheckedDB => check db evolution")
      sender ! checkDBEvolution()
    case x =>
      log.warning(s"uncheckedDB => unsupported command:$x")
  }

  def handleEntityInsert[E <: AnyRef, I <: BaseId: ClassTag](userId: UserId, entity: E, f: Long => I): Unit = {
    val clOf = classTag[I].runtimeClass.asInstanceOf[Class[I]]
    log.debug(s"created => Insert entity:$entity")
    val event = EntityInsertedEvent(metadata(userId), newId(clOf)(f), entity)
    state = state.copy(seqNr = state.seqNr + 1)
    persist(event)(afterEventPersisted)
    sender ! event
  }

  /**
   * Eventlog initialized, handle entity events
   */
  val created: Receive = {
    case e @ InsertEntityCommand(userId, entity: AbotypModify) =>
      handleEntityInsert[AbotypModify, AbotypId](userId, entity, AbotypId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotModify) =>
      handleEntityInsert[DepotModify, DepotId](userId, entity, DepotId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungModify) =>
      handleEntityInsert[DepotlieferungModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungModify) =>
      handleEntityInsert[HeimlieferungModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungModify) =>
      handleEntityInsert[PostlieferungModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungAbotypModify) =>
      handleEntityInsert[DepotlieferungAbotypModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungAbotypModify) =>
      handleEntityInsert[HeimlieferungAbotypModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungAbotypModify) =>
      handleEntityInsert[PostlieferungAbotypModify, VertriebsartId](userId, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungAboModify) =>
      handleEntityInsert[DepotlieferungAboModify, AboId](userId, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungAboModify) =>
      handleEntityInsert[HeimlieferungAboModify, AboId](userId, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungAboModify) =>
      handleEntityInsert[PostlieferungAboModify, AboId](userId, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: KundeModify) =>
      if (entity.ansprechpersonen.isEmpty) {
        //TODO: handle error
      } else {
        log.debug(s"created => Insert entity:$entity")
        val event = EntityInsertedEvent(metadata(userId), newId(classOf[KundeId])(KundeId.apply), entity)

        state = state.copy(seqNr = state.seqNr + 1)
        persist(event)(afterEventPersisted)

        val kundeId = event.id
        entity.ansprechpersonen.zipWithIndex.map {
          case (newPerson, index) =>
            val sort = index + 1
            val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> kundeId, "sort" -> sort)
            log.debug(s"created => Insert entity:$personCreate")
            val event = EntityInsertedEvent(metadata(userId), newId(classOf[PersonId])(PersonId.apply), personCreate)
            state = incState
            persist(event)(afterEventPersisted)
        }

        sender ! event
      }
    case e @ InsertEntityCommand(userId, entity: CustomKundentypCreate) =>
      handleEntityInsert[CustomKundentypCreate, CustomKundentypId](userId, entity, CustomKundentypId.apply)
    case e @ InsertEntityCommand(userId, entity: LieferungAbotypCreate) =>
      handleEntityInsert[LieferungAbotypCreate, LieferungId](userId, entity, LieferungId.apply)
    case e @ InsertEntityCommand(userId, entity: LieferplanungCreate) =>
      handleEntityInsert[LieferplanungCreate, LieferplanungId](userId, entity, LieferplanungId.apply)
    case e @ InsertEntityCommand(userId, entity: BestellungenCreate) =>
      handleEntityInsert[BestellungenCreate, BestellungId](userId, entity, BestellungId.apply)
    case e @ InsertEntityCommand(userId, entity: PendenzModify) =>
      handleEntityInsert[PendenzModify, PendenzId](userId, entity, PendenzId.apply)
    case e @ InsertEntityCommand(userId, entity: PersonCreate) =>
      handleEntityInsert[PersonCreate, PersonId](userId, entity, PersonId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktModify) =>
      handleEntityInsert[ProduktModify, ProduktId](userId, entity, ProduktId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktProduktekategorie) =>
      handleEntityInsert[ProduktProduktekategorie, ProduktProduktekategorieId](userId, entity, ProduktProduktekategorieId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktProduzent) =>
      handleEntityInsert[ProduktProduzent, ProduktProduzentId](userId, entity, ProduktProduzentId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktekategorieModify) =>
      handleEntityInsert[ProduktekategorieModify, ProduktekategorieId](userId, entity, ProduktekategorieId.apply)
    case e @ InsertEntityCommand(userId, entity: ProjektModify) =>
      handleEntityInsert[ProjektModify, ProjektId](userId, entity, ProjektId.apply)
    case e @ InsertEntityCommand(userId, entity: TourModify) =>
      handleEntityInsert[TourModify, TourId](userId, entity, TourId.apply)
    case e @ InsertEntityCommand(userId, entity: RechnungModify) =>
      handleEntityInsert[RechnungModify, RechnungId](userId, entity, RechnungId.apply)
    case UpdateEntityCommand(userId, id: KundeId, entity: KundeModify) =>
      val partitions = entity.ansprechpersonen.partition(_.id.isDefined)
      val newPersons: Seq[PersonModify] = partitions._2.zipWithIndex.map {
        case (newPerson, index) =>
          //generate persistent id for new person
          val sort = partitions._1.length + index
          val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> id, "sort" -> sort)
          val event = EntityInsertedEvent(metadata(userId), newId(classOf[PersonId])(PersonId.apply), personCreate)
          state = incState
          persist(event)(afterEventPersisted)
          newPerson.copy(id = Some(event.id))
      }
      val updatePersons = (partitions._1 ++ newPersons)
      val updateEntity = entity.copy(ansprechpersonen = updatePersons)
      state = incState
      persist(EntityUpdatedEvent(metadata(userId), id, updateEntity))(afterEventPersisted)
    case UpdateEntityCommand(userId, id, entity) =>
      log.debug(s"created => Update entity::$id, $entity")
      state = incState
      persist(EntityUpdatedEvent(metadata(userId), id, entity))(afterEventPersisted)
    case DeleteEntityCommand(userId, entity) =>
      log.debug(s"created => delete entity:$entity")
      state = incState
      persist(EntityDeletedEvent(metadata(userId), entity))(afterEventPersisted)
    case KillAggregate =>
      log.debug(s"created => KillAggregate")
      context.stop(self)
    case GetState =>
      log.debug(s"created => GetState")
      sender ! state
    case other =>
      log.warning(s"created => Received unknown command:$other")
  }

  def metadata(userId: UserId) = {
    EventMetadata(userId, VERSION, DateTime.now, state.seqNr, persistenceId)
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
