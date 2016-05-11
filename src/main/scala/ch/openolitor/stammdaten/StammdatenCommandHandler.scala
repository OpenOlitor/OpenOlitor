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
package ch.openolitor.stammdaten

import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import scala.util._
import scalikejdbc.DB
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.exceptions._
import akka.actor.ActorSystem
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.Macros._

object StammdatenCommandHandler {
  case class LieferplanungAbschliessenCommand(originator: UserId, id: LieferplanungId) extends UserCommand
  case class LieferplanungAbrechnenCommand(originator: UserId, id: LieferplanungId) extends UserCommand
  case class BestellungErneutVersenden(originator: UserId, id: BestellungId) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class BestellungVersendenEvent(meta: EventMetadata, id: BestellungId) extends PersistentEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler with StammdatenDBMappings with ConnectionPoolContextAware {
  self: StammdatenWriteRepositoryComponent =>
  import StammdatenCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventMetadata => Try[Seq[PersistentEvent]]] = {
    case LieferplanungAbschliessenCommand(userId, id: LieferplanungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Offen =>
              Success(Seq(LieferplanungAbschliessenEvent(meta, id)))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' abgeschlossen werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden")))
      }

    case LieferplanungAbrechnenCommand(userId, id: LieferplanungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Abgeschlossen =>
              Success(Seq(LieferplanungAbrechnenEvent(meta, id)))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Abgeschlossen' verrechnet werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden")))
      }

    case BestellungErneutVersenden(userId, id: BestellungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(bestellungMapping, id) map { bestellung =>
          bestellung.status match {
            case Offen | Abgeschlossen =>
              Success(Seq(BestellungVersendenEvent(meta, id)))
            case _ =>
              Failure(new InvalidStateException("Eine Bestellung kann nur in den Stati 'Offen' oder 'Abgeschlossen' erneut versendet werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Bestellung mit der Nr. $id gefunden")))
      }

    /*
       * Insert command handling
       */
    case e @ InsertEntityCommand(userId, entity: CustomKundentypCreate) => idFactory => meta =>
      handleEntityInsert[CustomKundentypCreate, CustomKundentypId](idFactory, meta, entity, CustomKundentypId.apply)
    case e @ InsertEntityCommand(userId, entity: LieferungAbotypCreate) => idFactory => meta =>
      handleEntityInsert[LieferungAbotypCreate, LieferungId](idFactory, meta, entity, LieferungId.apply)
    case e @ InsertEntityCommand(userId, entity: LieferplanungCreate) => idFactory => meta =>
      handleEntityInsert[LieferplanungCreate, LieferplanungId](idFactory, meta, entity, LieferplanungId.apply)
    case e @ InsertEntityCommand(userId, entity: LieferpositionenCreate) => idFactory => meta =>
      handleEntityInsert[LieferpositionenCreate, LieferpositionId](idFactory, meta, entity, LieferpositionId.apply)
    case e @ InsertEntityCommand(userId, entity: BestellungenCreate) => idFactory => meta =>
      handleEntityInsert[BestellungenCreate, BestellungId](idFactory, meta, entity, BestellungId.apply)
    case e @ InsertEntityCommand(userId, entity: PendenzModify) => idFactory => meta =>
      handleEntityInsert[PendenzModify, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(userId, entity: PersonCreate) => idFactory => meta =>
      handleEntityInsert[PersonCreate, PersonId](idFactory, meta, entity, PersonId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduzentModify) => idFactory => meta =>
      handleEntityInsert[ProduzentModify, ProduzentId](idFactory, meta, entity, ProduzentId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktModify) => idFactory => meta =>
      handleEntityInsert[ProduktModify, ProduktId](idFactory, meta, entity, ProduktId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktProduktekategorie) => idFactory => meta =>
      handleEntityInsert[ProduktProduktekategorie, ProduktProduktekategorieId](idFactory, meta, entity, ProduktProduktekategorieId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktProduzent) => idFactory => meta =>
      handleEntityInsert[ProduktProduzent, ProduktProduzentId](idFactory, meta, entity, ProduktProduzentId.apply)
    case e @ InsertEntityCommand(userId, entity: ProduktekategorieModify) => idFactory => meta =>
      handleEntityInsert[ProduktekategorieModify, ProduktekategorieId](idFactory, meta, entity, ProduktekategorieId.apply)
    case e @ InsertEntityCommand(userId, entity: ProjektModify) => idFactory => meta =>
      handleEntityInsert[ProjektModify, ProjektId](idFactory, meta, entity, ProjektId.apply)
    case e @ InsertEntityCommand(userId, entity: TourModify) => idFactory => meta =>
      handleEntityInsert[TourModify, TourId](idFactory, meta, entity, TourId.apply)
    case e @ InsertEntityCommand(userId, entity: AbwesenheitCreate) => idFactory => meta =>
      handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, entity, AbwesenheitId.apply)
    case e @ InsertEntityCommand(userId, entity: AbotypModify) => idFactory => meta =>
      handleEntityInsert[AbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotModify) => idFactory => meta =>
      handleEntityInsert[DepotModify, DepotId](idFactory, meta, entity, DepotId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(userId, entity: DepotlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: HeimlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: PostlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(userId, entity: PendenzCreate) => idFactory => meta =>
      handleEntityInsert[PendenzCreate, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(userId, entity: KundeModify) => idFactory => meta =>
      if (entity.ansprechpersonen.isEmpty) {
        Failure(new InvalidStateException(s"Zum Erstellen eines Kunden muss mindestens ein Ansprechpartner angegeben werden"))
      } else {
        logger.debug(s"created => Insert entity:$entity")
        val kundeEvent = EntityInsertedEvent(meta, KundeId(idFactory(classOf[KundeId])), entity)

        val kundeId = kundeEvent.id
        val apartnerEvents = entity.ansprechpersonen.zipWithIndex.map {
          case (newPerson, index) =>
            val sort = index + 1
            val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> kundeId, "sort" -> sort)
            logger.debug(s"created => Insert entity:$personCreate")
            EntityInsertedEvent(meta, PersonId(idFactory(classOf[PersonId])), personCreate)
        }

        Success(kundeEvent +: apartnerEvents)
      }

    /*
    * Custom update command handling
    */
    case UpdateEntityCommand(userId, id: KundeId, entity: KundeModify) => idFactory => meta =>
      val partitions = entity.ansprechpersonen.partition(_.id.isDefined)
      val newPersons = partitions._2.zipWithIndex.map {
        case (newPerson, index) =>
          //generate persistent id for new person
          val sort = partitions._1.length + index
          val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> id, "sort" -> sort)
          val event = EntityInsertedEvent(meta, PersonId(idFactory(classOf[PersonId])), personCreate)
          (event, newPerson.copy(id = Some(event.id)))
      }
      val newPersonsEvents = newPersons.map(_._1)
      val updatePersons = (partitions._1 ++ newPersons.map(_._2))

      val pendenzenPartitions = entity.pendenzen.partition(_.id.isDefined)
      val newPendenzen = pendenzenPartitions._2.map {
        case newPendenz =>
          val pendenzCreate = copyTo[PendenzModify, PendenzCreate](newPendenz, "kundeId" -> id)
          val event = EntityInsertedEvent(meta, PendenzId(idFactory(classOf[PendenzId])), pendenzCreate)
          (event, newPendenz.copy(id = Some(event.id)))
      }
      val newPendenzenEvents = newPendenzen.map(_._1)
      val updatePendenzen = (pendenzenPartitions._1 ++ newPendenzen.map(_._2))

      val updateEntity = entity.copy(ansprechpersonen = updatePersons, pendenzen = updatePendenzen)
      val updateEvent = EntityUpdatedEvent(meta, id, updateEntity)
      Success(updateEvent +: (newPersonsEvents ++ newPendenzenEvents))
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCommandHandler
    with DefaultStammdatenWriteRepositoryComponent {
}
