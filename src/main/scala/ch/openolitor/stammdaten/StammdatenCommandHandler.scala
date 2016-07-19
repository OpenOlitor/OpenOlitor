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
import com.fasterxml.jackson.databind.JsonSerializable
import ch.openolitor.buchhaltung.models.RechnungCreate
import ch.openolitor.buchhaltung.models.RechnungId

object StammdatenCommandHandler {
  case class LieferplanungAbschliessenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand
  case class LieferplanungAbrechnenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand
  case class BestellungAnProduzentenVersenden(originator: PersonId, id: BestellungId) extends UserCommand
  case class PasswortWechselCommand(originator: PersonId, personId: PersonId, passwort: Array[Char]) extends UserCommand
  case class AuslieferungenAlsAusgeliefertMarkierenCommand(originator: PersonId, ids: Seq[AuslieferungId]) extends UserCommand
  case class CreateAnzahlLieferungenRechnungenCommand(originator: PersonId, rechnungCreate: AboRechnungCreate) extends UserCommand
  case class CreateBisGuthabenRechnungenCommand(originator: PersonId, rechnungCreate: AboRechnungCreate) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class BestellungVersendenEvent(meta: EventMetadata, id: BestellungId) extends PersistentEvent with JSONSerializable
  case class PasswortGewechseltEvent(meta: EventMetadata, personId: PersonId, passwort: Array[Char]) extends PersistentEvent with JSONSerializable
  case class AuslieferungAlsAusgeliefertMarkierenEvent(meta: EventMetadata, id: AuslieferungId) extends PersistentEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler with StammdatenDBMappings with ConnectionPoolContextAware {
  self: StammdatenWriteRepositoryComponent =>
  import StammdatenCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventMetadata => Try[Seq[PersistentEvent]]] = {
    case LieferplanungAbschliessenCommand(personId, id) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Offen =>
              val bestellungId = BestellungId(idFactory(classOf[BestellungId]))
              val insertEvent = EntityInsertedEvent(meta, bestellungId, BestellungenCreate(id))
              val lpAbschliessenEvent = LieferplanungAbschliessenEvent(meta, id)
              val bestellungVersendenEvent = BestellungVersendenEvent(meta, bestellungId)
              Success(Seq(insertEvent, lpAbschliessenEvent, bestellungVersendenEvent))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' abgeschlossen werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden")))
      }

    case LieferplanungAbrechnenCommand(personId, id: LieferplanungId) => idFactory => meta =>
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

    case BestellungAnProduzentenVersenden(personId, id: BestellungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(bestellungMapping, id) map { bestellung =>
          bestellung.status match {
            case Offen | Abgeschlossen =>
              Success(Seq(BestellungVersendenEvent(meta, id)))
            case _ =>
              Failure(new InvalidStateException("Eine Bestellung kann nur in den Stati 'Offen' oder 'Abgeschlossen' versendet werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Bestellung mit der Nr. $id gefunden")))
      }

    case AuslieferungenAlsAusgeliefertMarkierenCommand(personId, ids: Seq[AuslieferungId]) => idFactory => meta =>
      DB readOnly { implicit session =>
        val (events, failures) = ids map { id =>
          stammdatenWriteRepository.getById(depotAuslieferungMapping, id) orElse
            stammdatenWriteRepository.getById(tourAuslieferungMapping, id) orElse
            stammdatenWriteRepository.getById(postAuslieferungMapping, id) map { auslieferung =>
              auslieferung.status match {
                case Erfasst =>
                  Success(AuslieferungAlsAusgeliefertMarkierenEvent(meta, id))
                case _ =>
                  Failure(new InvalidStateException(s"Eine Auslieferung kann nur im Status 'Erfasst' als ausgeliefert markiert werden. Nr. $id"))
              }
            } getOrElse (Failure(new InvalidStateException(s"Keine Auslieferung mit der Nr. $id gefunden")))
        } partition (_.isSuccess)

        if (events.isEmpty) {
          Failure(new InvalidStateException(s"Keine der Auslieferungen konnte abgearbeitet werden"))
        } else {
          Success(events map (_.get))
        }
      }

    case CreateAnzahlLieferungenRechnungenCommand(originator, rechnungCreate) => idFactory => meta =>
      createAboRechnungen(idFactory, meta, rechnungCreate)

    case CreateBisGuthabenRechnungenCommand(originator, rechnungCreate) => idFactory => meta =>
      createAboRechnungen(idFactory, meta, rechnungCreate)

    case PasswortWechselCommand(originator, personId, pwd) => idFactory => meta =>
      Success(Seq(PasswortGewechseltEvent(meta, personId, pwd)))

    /*
       * Insert command handling
       */
    case e @ InsertEntityCommand(personId, entity: CustomKundentypCreate) => idFactory => meta =>
      handleEntityInsert[CustomKundentypCreate, CustomKundentypId](idFactory, meta, entity, CustomKundentypId.apply)
    case e @ InsertEntityCommand(personId, entity: LieferungenAbotypCreate) => idFactory => meta =>
      val events = entity.daten.map { datum =>
        val lieferungCreate = copyTo[LieferungenAbotypCreate, LieferungAbotypCreate](entity, "datum" -> datum)
        insertEntityEvent[LieferungAbotypCreate, LieferungId](idFactory, meta, lieferungCreate, LieferungId.apply)
      }
      Success(events)
    case e @ InsertEntityCommand(personId, entity: LieferungAbotypCreate) => idFactory => meta =>
      handleEntityInsert[LieferungAbotypCreate, LieferungId](idFactory, meta, entity, LieferungId.apply)
    case e @ InsertEntityCommand(personId, entity: LieferplanungCreate) => idFactory => meta =>
      handleEntityInsert[LieferplanungCreate, LieferplanungId](idFactory, meta, entity, LieferplanungId.apply)
    case e @ InsertEntityCommand(personId, entity: LieferpositionenModify) => idFactory => meta =>
      handleEntityInsert[LieferpositionenModify, LieferpositionId](idFactory, meta, entity, LieferpositionId.apply)
    case e @ InsertEntityCommand(personId, entity: BestellungenCreate) => idFactory => meta =>
      handleEntityInsert[BestellungenCreate, BestellungId](idFactory, meta, entity, BestellungId.apply)
    case e @ InsertEntityCommand(personId, entity: PendenzModify) => idFactory => meta =>
      handleEntityInsert[PendenzModify, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(personId, entity: PersonCreate) => idFactory => meta =>
      handleEntityInsert[PersonCreate, PersonId](idFactory, meta, entity, PersonId.apply)
    case e @ InsertEntityCommand(personId, entity: ProduzentModify) => idFactory => meta =>
      handleEntityInsert[ProduzentModify, ProduzentId](idFactory, meta, entity, ProduzentId.apply)
    case e @ InsertEntityCommand(personId, entity: ProduktModify) => idFactory => meta =>
      handleEntityInsert[ProduktModify, ProduktId](idFactory, meta, entity, ProduktId.apply)
    case e @ InsertEntityCommand(personId, entity: ProduktProduktekategorie) => idFactory => meta =>
      handleEntityInsert[ProduktProduktekategorie, ProduktProduktekategorieId](idFactory, meta, entity, ProduktProduktekategorieId.apply)
    case e @ InsertEntityCommand(personId, entity: ProduktProduzent) => idFactory => meta =>
      handleEntityInsert[ProduktProduzent, ProduktProduzentId](idFactory, meta, entity, ProduktProduzentId.apply)
    case e @ InsertEntityCommand(personId, entity: ProduktekategorieModify) => idFactory => meta =>
      handleEntityInsert[ProduktekategorieModify, ProduktekategorieId](idFactory, meta, entity, ProduktekategorieId.apply)
    case e @ InsertEntityCommand(personId, entity: ProjektModify) => idFactory => meta =>
      handleEntityInsert[ProjektModify, ProjektId](idFactory, meta, entity, ProjektId.apply)
    case e @ InsertEntityCommand(personId, entity: TourModify) => idFactory => meta =>
      handleEntityInsert[TourModify, TourId](idFactory, meta, entity, TourId.apply)
    case e @ InsertEntityCommand(personId, entity: AbwesenheitCreate) => idFactory => meta =>
      handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, entity, AbwesenheitId.apply)
    case e @ InsertEntityCommand(personId, entity: AbotypModify) => idFactory => meta =>
      handleEntityInsert[AbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(personId, entity: DepotModify) => idFactory => meta =>
      handleEntityInsert[DepotModify, DepotId](idFactory, meta, entity, DepotId.apply)
    case e @ InsertEntityCommand(personId, entity: DepotlieferungModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: HeimlieferungModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: PostlieferungModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: DepotlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: HeimlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: PostlieferungAbotypModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungAbotypModify, VertriebsartId](idFactory, meta, entity, VertriebsartId.apply)
    case e @ InsertEntityCommand(personId, entity: DepotlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[DepotlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(personId, entity: HeimlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[HeimlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(personId, entity: PostlieferungAboModify) => idFactory => meta =>
      handleEntityInsert[PostlieferungAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(personId, entity: PendenzCreate) => idFactory => meta =>
      handleEntityInsert[PendenzCreate, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(personId, entity: VertriebModify) => idFactory => meta =>
      handleEntityInsert[VertriebModify, VertriebId](idFactory, meta, entity, VertriebId.apply)
    case e @ InsertEntityCommand(personId, entity: KundeModify) => idFactory => meta =>
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
    case UpdateEntityCommand(personId, id: KundeId, entity: KundeModify) => idFactory => meta =>
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
          val pendenzCreate = copyTo[PendenzModify, PendenzCreate](newPendenz, "kundeId" -> id, "generiert" -> FALSE)
          val event = EntityInsertedEvent(meta, PendenzId(idFactory(classOf[PendenzId])), pendenzCreate)
          (event, newPendenz.copy(id = Some(event.id)))
      }
      val newPendenzenEvents = newPendenzen.map(_._1)
      val updatePendenzen = (pendenzenPartitions._1 ++ newPendenzen.map(_._2))

      val updateEntity = entity.copy(ansprechpersonen = updatePersons, pendenzen = updatePendenzen)
      val updateEvent = EntityUpdatedEvent(meta, id, updateEntity)
      Success(updateEvent +: (newPersonsEvents ++ newPendenzenEvents))

    case UpdateEntityCommand(personId, id: AboId, entity: AboGuthabenModify) => idFactory => meta =>
      //TODO: assemble text using gettext
      val title = "Guthaben angepasst. Abo Nr.:" + id.id + ", Grund:"
      val pendenzEvent = addKundenPendenz(idFactory, meta, id, title + entity.bemerkung)
      Success(Seq(Some(EntityUpdatedEvent(meta, id, entity)), pendenzEvent).flatten)
    case UpdateEntityCommand(personId, id: AboId, entity: AboVertriebsartModify) => idFactory => meta =>
      //TODO: assemble text using gettext
      val title = "Vertriebsart angepasst. Abo Nr.:" + id.id + ", Grund:"
      val pendenzEvent = addKundenPendenz(idFactory, meta, id, title + entity.bemerkung)
      Success(Seq(Some(EntityUpdatedEvent(meta, id, entity)), pendenzEvent).flatten)
  }

  def addKundenPendenz(idFactory: IdFactory, meta: EventMetadata, id: AboId, bemerkung: String): Option[PersistentEvent] = {
    DB readOnly { implicit session =>
      // zusätzlich eine pendenz erstellen      
      ((stammdatenWriteRepository.getById(depotlieferungAboMapping, id) map { abo =>
        DepotlieferungAboModify
        abo.kundeId
      }) orElse (stammdatenWriteRepository.getById(heimlieferungAboMapping, id) map { abo =>
        abo.kundeId
      }) orElse (stammdatenWriteRepository.getById(postlieferungAboMapping, id) map { abo =>
        abo.kundeId
      })) map { kundeId =>
        //TODO: assemble text using gettext
        val title = "Guthaben angepasst: "
        val pendenzCreate = PendenzCreate(kundeId, meta.timestamp, Some(bemerkung), Erledigt, true)
        EntityInsertedEvent[PendenzId, PendenzCreate](meta, PendenzId(idFactory(classOf[PendenzId])), pendenzCreate)
      }
    }
  }

  def createAboRechnungen(idFactory: IdFactory, meta: EventMetadata, rechnungCreate: AboRechnungCreate) = {
    DB readOnly { implicit session =>
      val (events, failures) = rechnungCreate.ids map { id =>
        stammdatenWriteRepository.getAboDetail(id) flatMap { aboDetail =>
          stammdatenWriteRepository.getById(abotypMapping, aboDetail.abotypId) flatMap { abotyp =>
            stammdatenWriteRepository.getById(kundeMapping, aboDetail.kundeId) map { kunde =>

              // TODO check preisEinheit
              if (abotyp.preiseinheit != ProLieferung) {
                Failure(new InvalidStateException(s"Für den Abotyp dieses Abos ($id) kann keine Guthabenrechnung erstellt werden"))
              } else {
                // has to be refactored as soon as more modes are available
                val anzahlLieferungen = rechnungCreate.anzahlLieferungen getOrElse {
                  math.max(((rechnungCreate.bisGuthaben getOrElse aboDetail.guthaben) - aboDetail.guthaben), 0)
                }

                if (anzahlLieferungen > 0) {
                  val betrag = rechnungCreate.betrag getOrElse abotyp.preis * anzahlLieferungen

                  val rechnung = RechnungCreate(
                    aboDetail.kundeId,
                    id,
                    rechnungCreate.titel,
                    anzahlLieferungen,
                    rechnungCreate.waehrung,
                    betrag,
                    None,
                    rechnungCreate.rechnungsDatum,
                    rechnungCreate.faelligkeitsDatum,
                    None,
                    kunde.strasseLieferung getOrElse kunde.strasse,
                    kunde.hausNummerLieferung orElse kunde.hausNummer,
                    kunde.adressZusatzLieferung orElse kunde.adressZusatz,
                    kunde.plzLieferung getOrElse kunde.plz,
                    kunde.ortLieferung getOrElse kunde.ort
                  )

                  Success(insertEntityEvent(idFactory, meta, rechnung, RechnungId.apply))
                } else {
                  Failure(new InvalidStateException(s"Für das Abo mit der Id $id wurde keine Rechnung erstellt. Anzahl Lieferungen 0"))
                }
              }
            }
          }
        } getOrElse (Failure(new InvalidStateException(s"Für das Abo mit der Id $id konnte keine Rechnung erstellt werden.")))
      } partition (_.isSuccess)

      if (events.isEmpty) {
        Failure(new InvalidStateException(s"Keine der Rechnungen konnte erstellt werden"))
      } else {
        Success(events map (_.get))
      }
    }
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCommandHandler
    with DefaultStammdatenWriteRepositoryComponent {
}
