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

import ch.openolitor.buchhaltung.models.{ RechnungsPositionStatus, RechnungsPositionTyp }
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import scala.util._
import scalikejdbc.DB
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.exceptions._
import akka.actor.ActorSystem
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.Macros._
import ch.openolitor.buchhaltung.models.RechnungsPositionCreate
import ch.openolitor.buchhaltung.models.RechnungsPositionId
import org.joda.time.DateTime
import java.util.UUID
import scalikejdbc.DBSession
import ch.openolitor.util.IdUtil

object StammdatenCommandHandler {
  case class LieferplanungAbschliessenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand
  case class LieferplanungModifyCommand(originator: PersonId, lieferplanungModify: LieferplanungPositionenModify) extends UserCommand
  case class LieferplanungAbrechnenCommand(originator: PersonId, id: LieferplanungId) extends UserCommand
  case class AbwesenheitCreateCommand(originator: PersonId, abw: AbwesenheitCreate) extends UserCommand
  case class SammelbestellungAnProduzentenVersendenCommand(originator: PersonId, id: SammelbestellungId) extends UserCommand
  case class PasswortWechselCommand(originator: PersonId, personId: PersonId, passwort: Array[Char], einladung: Option[EinladungId]) extends UserCommand
  case class AuslieferungenAlsAusgeliefertMarkierenCommand(originator: PersonId, ids: Seq[AuslieferungId]) extends UserCommand
  case class CreateAnzahlLieferungenRechnungsPositionenCommand(originator: PersonId, aboRechnungCreate: AboRechnungsPositionBisAnzahlLieferungenCreate) extends UserCommand
  case class CreateBisGuthabenRechnungsPositionenCommand(originator: PersonId, aboRechnungCreate: AboRechnungsPositionBisGuthabenCreate) extends UserCommand
  case class LoginDeaktivierenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand
  case class LoginAktivierenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand
  case class EinladungSendenCommand(originator: PersonId, kundeId: KundeId, personId: PersonId) extends UserCommand
  case class SammelbestellungenAlsAbgerechnetMarkierenCommand(originator: PersonId, datum: DateTime, ids: Seq[SammelbestellungId]) extends UserCommand
  case class PasswortResetCommand(originator: PersonId, personId: PersonId) extends UserCommand
  case class RolleWechselnCommand(originator: PersonId, kundeId: KundeId, personId: PersonId, rolle: Rolle) extends UserCommand

  // TODO person id for calculations
  case class AboAktivierenCommand(aboId: AboId, originator: PersonId = PersonId(100)) extends UserCommand
  case class AboDeaktivierenCommand(aboId: AboId, originator: PersonId = PersonId(100)) extends UserCommand

  case class DeleteAbwesenheitCommand(originator: PersonId, id: AbwesenheitId) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungDataModifiedEvent(meta: EventMetadata, result: LieferplanungDataModify) extends PersistentEvent with JSONSerializable
  case class AbwesenheitCreateEvent(meta: EventMetadata, id: AbwesenheitId, abw: AbwesenheitCreate) extends PersistentEvent with JSONSerializable
  @Deprecated
  case class BestellungVersendenEvent(meta: EventMetadata, id: BestellungId) extends PersistentEvent with JSONSerializable
  case class SammelbestellungVersendenEvent(meta: EventMetadata, id: SammelbestellungId) extends PersistentEvent with JSONSerializable
  case class PasswortGewechseltEvent(meta: EventMetadata, personId: PersonId, passwort: Array[Char], einladungId: Option[EinladungId]) extends PersistentEvent with JSONSerializable
  case class LoginDeaktiviertEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId) extends PersistentEvent with JSONSerializable
  case class LoginAktiviertEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId) extends PersistentEvent with JSONSerializable
  case class EinladungGesendetEvent(meta: EventMetadata, einladung: EinladungCreate) extends PersistentEvent with JSONSerializable
  case class AuslieferungAlsAusgeliefertMarkierenEvent(meta: EventMetadata, id: AuslieferungId) extends PersistentEvent with JSONSerializable
  @Deprecated
  case class BestellungAlsAbgerechnetMarkierenEvent(meta: EventMetadata, datum: DateTime, id: BestellungId) extends PersistentEvent with JSONSerializable
  case class SammelbestellungAlsAbgerechnetMarkierenEvent(meta: EventMetadata, datum: DateTime, id: SammelbestellungId) extends PersistentEvent with JSONSerializable
  case class PasswortResetGesendetEvent(meta: EventMetadata, einladung: EinladungCreate) extends PersistentEvent with JSONSerializable
  case class RolleGewechseltEvent(meta: EventMetadata, kundeId: KundeId, personId: PersonId, rolle: Rolle) extends PersistentEvent with JSONSerializable

  case class AboAktiviertEvent(meta: EventMetadata, aboId: AboId) extends PersistentGeneratedEvent with JSONSerializable
  case class AboDeaktiviertEvent(meta: EventMetadata, aboId: AboId) extends PersistentGeneratedEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler with StammdatenDBMappings with ConnectionPoolContextAware with LieferungDurchschnittspreisHandler {
  self: StammdatenReadRepositorySyncComponent =>
  import StammdatenCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {

    case DeleteAbwesenheitCommand(personId, id) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.getLieferung(id) map { lieferung =>
          lieferung.status match {
            case (Offen | Ungeplant) =>
              Success(Seq(EntityDeleteEvent(id)))
            case _ =>
              Failure(new InvalidStateException("Die der Abwesenheit zugeordnete Lieferung muss Offen oder Ungeplant sein."))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Lieferung zu Abwesenheit Nr. $id gefunden"))
      }

    case LieferplanungAbschliessenCommand(personId, id) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Offen =>
              stammdatenReadRepository.countEarlierLieferungOffen(id) match {
                case Some(0) =>
                  val distinctSammelbestellungen = getDistinctSammelbestellungModifyByLieferplan(lieferplanung.id)

                  val bestellEvents = distinctSammelbestellungen.map { sammelbestellungCreate =>
                    val insertEvent = EntityInsertEvent(idFactory.newId(SammelbestellungId.apply), sammelbestellungCreate)

                    // TODO OO-589
                    //val bestellungVersendenEvent = SammelbestellungVersendenEvent(factory.newMetadata(meta), sammelbestellungId)
                    Seq(insertEvent) //, bestellungVersendenEvent)
                  }.toSeq.flatten

                  val lpAbschliessenEvent = DefaultResultingEvent(factory => LieferplanungAbschliessenEvent(factory.newMetadata(), id))

                  val createAuslieferungHeimEvent = getCreateAuslieferungHeimEvent(lieferplanung)(personId, session)
                  val createAuslieferungDepotPostEvent = getCreateDepotAuslieferungAndPostAusliferungEvent(lieferplanung)(personId, session)
                  Success(lpAbschliessenEvent +: bestellEvents ++: createAuslieferungHeimEvent ++: createAuslieferungDepotPostEvent)
                case _ =>
                  Failure(new InvalidStateException("Es dürfen keine früheren Lieferungen in offnen Lieferplanungen hängig sein."))
              }
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' abgeschlossen werden"))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden"))
      }

    case LieferplanungModifyCommand(personId, lieferplanungPositionenModify) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.getById(lieferplanungMapping, lieferplanungPositionenModify.id) map { lieferplanung =>
          lieferplanung.status match {
            case state @ (Offen | Abgeschlossen) =>
              stammdatenReadRepository.countEarlierLieferungOffen(lieferplanungPositionenModify.id) match {
                case Some(0) =>
                  val missingSammelbestellungen = if (state == Abgeschlossen) {
                    // get existing sammelbestellungen
                    val existingSammelbestellungen = (stammdatenReadRepository.getSammelbestellungen(lieferplanungPositionenModify.id) map { sammelbestellung =>
                      SammelbestellungModify(sammelbestellung.produzentId, lieferplanung.id, sammelbestellung.datum)
                    }).toSet

                    // get distinct sammelbestellungen by lieferplanung
                    val distinctSammelbestellungen = getDistinctSammelbestellungModifyByLieferplan(lieferplanung.id)

                    // evaluate which sammelbestellungen are missing and have to be inserted
                    // they will be used in handleLieferungChanged afterwards
                    (distinctSammelbestellungen -- existingSammelbestellungen).map { s =>
                      SammelbestellungCreate(idFactory.newId(SammelbestellungId.apply), s.produzentId, s.lieferplanungId, s.datum)
                    }.toSeq
                  } else {
                    Nil
                  }

                  Success(DefaultResultingEvent(factory => LieferplanungDataModifiedEvent(factory.newMetadata(), LieferplanungDataModify(lieferplanungPositionenModify.id, missingSammelbestellungen.toSet, lieferplanungPositionenModify.lieferungen))) :: Nil)
                case _ =>
                  Failure(new InvalidStateException("Es dürfen keine früheren Lieferungen in offnen Lieferplanungen hängig sein."))
              }
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' oder 'Abgeschlossen' aktualisiert werden"))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. ${lieferplanungPositionenModify.id} gefunden"))
      }

    case LieferplanungAbrechnenCommand(personId, id: LieferplanungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Abgeschlossen =>
              Success(Seq(DefaultResultingEvent(factory => LieferplanungAbrechnenEvent(factory.newMetadata(), id))))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Abgeschlossen' verrechnet werden"))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden"))
      }

    case AbwesenheitCreateCommand(personId, abw: AbwesenheitCreate) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.countAbwesend(abw.lieferungId, abw.aboId) match {
          case Some(0) =>
            handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, abw, AbwesenheitId.apply)
          case _ =>
            Failure(new InvalidStateException("Eine Abwesenheit kann nur einmal erfasst werden"))
        }
      }

    case SammelbestellungAnProduzentenVersendenCommand(personId, id: SammelbestellungId) => idFactory => meta =>
      DB readOnly { implicit session =>
        stammdatenReadRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
          sammelbestellung.status match {
            case Offen | Abgeschlossen =>
              Success(Seq(DefaultResultingEvent(factory => SammelbestellungVersendenEvent(factory.newMetadata(), id))))
            case _ =>
              Failure(new InvalidStateException("Eine Bestellung kann nur in den Status 'Offen' oder 'Abgeschlossen' versendet werden"))
          }
        } getOrElse Failure(new InvalidStateException(s"Keine Bestellung mit der Nr. $id gefunden"))
      }

    case AuslieferungenAlsAusgeliefertMarkierenCommand(personId, ids: Seq[AuslieferungId]) => idFactory => meta =>
      DB readOnly { implicit session =>
        val (events, failures) = ids map { id =>
          stammdatenReadRepository.getById(depotAuslieferungMapping, id) orElse
            stammdatenReadRepository.getById(tourAuslieferungMapping, id) orElse
            stammdatenReadRepository.getById(postAuslieferungMapping, id) map { auslieferung =>
              auslieferung.status match {
                case Erfasst =>
                  val copy = auslieferung match {
                    case d: DepotAuslieferung =>
                      d.copy(status = Ausgeliefert)
                    case t: TourAuslieferung =>
                      t.copy(status = Ausgeliefert)
                    case p: PostAuslieferung =>
                      p.copy(status = Ausgeliefert)
                  }
                  Success(EntityUpdateEvent(id, copy))
                case _ =>
                  Failure(new InvalidStateException(s"Eine Auslieferung kann nur im Status 'Erfasst' als ausgeliefert markiert werden. Nr. $id"))
              }
            } getOrElse Failure(new InvalidStateException(s"Keine Auslieferung mit der Nr. $id gefunden"))
        } partition (_.isSuccess)

        if (events.isEmpty) {
          Failure(new InvalidStateException(s"Keine der Auslieferungen konnte abgearbeitet werden"))
        } else {
          Success(events map (_.get))
        }
      }

    case SammelbestellungenAlsAbgerechnetMarkierenCommand(personId, datum, ids: Seq[SammelbestellungId]) => idFactory => meta =>
      DB readOnly { implicit session =>
        val (events, failures) = ids map { id =>
          stammdatenReadRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
            sammelbestellung.status match {
              case Abgeschlossen =>
                Success(DefaultResultingEvent(factory => SammelbestellungAlsAbgerechnetMarkierenEvent(factory.newMetadata(), datum, id)))
              case _ =>
                Failure(new InvalidStateException(s"Eine Sammelbestellung kann nur im Status 'Abgeschlossen' als abgerechnet markiert werden. Nr. $id"))
            }
          } getOrElse Failure(new InvalidStateException(s"Keine Sammelbestellung mit der Nr. $id gefunden"))
        } partition (_.isSuccess)

        if (events.isEmpty) {
          Failure(new InvalidStateException(s"Keine der Sammelbestellung konnte abgearbeitet werden"))
        } else {
          Success(events map (_.get))
        }
      }

    case CreateAnzahlLieferungenRechnungsPositionenCommand(originator, aboRechnungCreate) => idFactory => meta =>
      createAboRechnungsPositionenAnzahlLieferungen(idFactory, meta, aboRechnungCreate)

    case CreateBisGuthabenRechnungsPositionenCommand(originator, aboRechnungCreate) => idFactory => meta =>
      createAboRechnungsPositionenBisGuthaben(idFactory, meta, aboRechnungCreate)

    case PasswortWechselCommand(originator, personId, pwd, einladungId) => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => PasswortGewechseltEvent(factory.newMetadata(), personId, pwd, einladungId))))

    case LoginDeaktivierenCommand(originator, kundeId, personId) if originator.id != personId => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => LoginDeaktiviertEvent(factory.newMetadata(), kundeId, personId))))

    case LoginAktivierenCommand(originator, kundeId, personId) if originator.id != personId => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => LoginAktiviertEvent(factory.newMetadata(), kundeId, personId))))

    case EinladungSendenCommand(originator, kundeId, personId) if originator.id != personId => idFactory => meta =>
      sendEinladung(idFactory, meta, kundeId, personId)

    case PasswortResetCommand(originator, personId) => idFactory => meta =>
      sendPasswortReset(idFactory, meta, personId)

    case RolleWechselnCommand(originator, kundeId, personId, rolle) if originator.id != personId => idFactory => meta =>
      changeRolle(idFactory, meta, kundeId, personId, rolle)

    case AboAktivierenCommand(aboId, originator) => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => AboAktiviertEvent(factory.newMetadata(), aboId))))

    case AboDeaktivierenCommand(aboId, originator) => idFactory => meta =>
      Success(Seq(DefaultResultingEvent(factory => AboDeaktiviertEvent(factory.newMetadata(), aboId))))

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
    case e @ InsertEntityCommand(personId, entity: LieferungPlanungAdd) => idFactory => meta =>
      handleEntityInsert[LieferungPlanungAdd, LieferungId](idFactory, meta, entity, LieferungId.apply)
    case e @ InsertEntityCommand(personId, entity: LieferpositionenModify) => idFactory => meta =>
      handleEntityInsert[LieferpositionenModify, LieferpositionId](idFactory, meta, entity, LieferpositionId.apply)
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
    case e @ InsertEntityCommand(personId, entity: TourCreate) => idFactory => meta =>
      handleEntityInsert[TourCreate, TourId](idFactory, meta, entity, TourId.apply)
    case e @ InsertEntityCommand(personId, entity: AbwesenheitCreate) => idFactory => meta =>
      handleEntityInsert[AbwesenheitCreate, AbwesenheitId](idFactory, meta, entity, AbwesenheitId.apply)
    case e @ InsertEntityCommand(personId, entity: AbotypModify) => idFactory => meta =>
      handleEntityInsert[AbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(personId, entity: ZusatzAbotypModify) => idFactory => meta =>
      handleEntityInsert[ZusatzAbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
    case e @ InsertEntityCommand(personId, entity: ZusatzAbotypModify) => idFactory => meta =>
      handleEntityInsert[ZusatzAbotypModify, AbotypId](idFactory, meta, entity, AbotypId.apply)
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
    case e @ InsertEntityCommand(personId, entity: ZusatzAboModify) => idFactory => meta =>
      handleEntityInsert[ZusatzAboModify, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(personId, entity: ZusatzAboCreate) => idFactory => meta =>
      handleEntityInsert[ZusatzAboCreate, AboId](idFactory, meta, entity, AboId.apply)
    case e @ InsertEntityCommand(personId, entity: PendenzCreate) => idFactory => meta =>
      handleEntityInsert[PendenzCreate, PendenzId](idFactory, meta, entity, PendenzId.apply)
    case e @ InsertEntityCommand(personId, entity: VertriebModify) => idFactory => meta =>
      handleEntityInsert[VertriebModify, VertriebId](idFactory, meta, entity, VertriebId.apply)
    case e @ InsertEntityCommand(personId, entity: ProjektVorlageCreate) => idFactory => meta =>
      handleEntityInsert[ProjektVorlageCreate, ProjektVorlageId](idFactory, meta, entity, ProjektVorlageId.apply)
    case e @ InsertEntityCommand(personId, entity: KundeModify) => idFactory => meta =>
      if (entity.ansprechpersonen.isEmpty) {
        Failure(new InvalidStateException(s"Zum Erstellen eines Kunden muss mindestens ein Ansprechpartner angegeben werden"))
      } else {
        logger.debug(s"created => Insert entity:$entity")
        val kundeEvent = EntityInsertEvent(idFactory.newId(KundeId.apply), entity)

        val kundeId = kundeEvent.id
        val apartnerEvents = entity.ansprechpersonen.zipWithIndex.map {
          case (newPerson, index) =>
            val sort = index + 1
            val personCreate = copyTo[PersonModify, PersonCreate](newPerson, "kundeId" -> kundeId, "sort" -> sort)
            logger.debug(s"created => Insert entity:$personCreate")
            EntityInsertEvent(idFactory.newId(PersonId.apply), personCreate)
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
          val event = EntityInsertEvent(idFactory.newId(PersonId.apply), personCreate)
          (event, newPerson.copy(id = Some(event.id)))
      }
      val newPersonsEvents = newPersons.map(_._1)
      val updatePersons = (partitions._1 ++ newPersons.map(_._2))

      val pendenzenPartitions = entity.pendenzen.partition(_.id.isDefined)
      val newPendenzen = pendenzenPartitions._2.map {
        case newPendenz =>
          val pendenzCreate = copyTo[PendenzModify, PendenzCreate](newPendenz, "kundeId" -> id, "generiert" -> FALSE)
          val event = EntityInsertEvent(idFactory.newId(PendenzId.apply), pendenzCreate)
          (event, newPendenz.copy(id = Some(event.id)))
      }
      val newPendenzenEvents = newPendenzen.map(_._1)
      val updatePendenzen = (pendenzenPartitions._1 ++ newPendenzen.map(_._2))

      val updateEntity = entity.copy(ansprechpersonen = updatePersons, pendenzen = updatePendenzen)
      val updateEvent = EntityUpdateEvent(id, updateEntity)
      Success(updateEvent +: (newPersonsEvents ++ newPendenzenEvents))

    case UpdateEntityCommand(personId, id: AboId, entity: AboGuthabenModify) => idFactory => meta =>
      DB readOnly { implicit session =>
        //TODO: assemble text using gettext
        stammdatenReadRepository.getAboDetail(id) match {
          case Some(abo) => {
            val text = s"Guthaben manuell angepasst. Abo Nr.: ${id.id}; Bisher: ${abo.guthaben}; Neu: ${entity.guthabenNeu}; Grund: ${entity.bemerkung}"
            val pendenzEvent = addKundenPendenz(idFactory, meta, id, text)
            Success(Seq(Some(EntityUpdateEvent(id, entity)), pendenzEvent).flatten)
          }
          case None =>
            Failure(new InvalidStateException(s"UpdateEntityCommand: Abo konnte nicht gefunden werden"))
        }
      }
    case UpdateEntityCommand(personId, id: AboId, entity: AboVertriebsartModify) => idFactory => meta =>
      //TODO: assemble text using gettext
      val text = s"Vertriebsart angepasst. Abo Nr.: ${id.id}, Neu: ${entity.vertriebsartIdNeu}; Grund: ${entity.bemerkung}"
      val pendenzEvent = addKundenPendenz(idFactory, meta, id, text)
      Success(Seq(Some(EntityUpdateEvent(id, entity)), pendenzEvent).flatten)
  }

  def addKundenPendenz(idFactory: IdFactory, meta: EventTransactionMetadata, id: AboId, bemerkung: String): Option[ResultingEvent] = {
    DB readOnly { implicit session =>
      // zusätzlich eine pendenz erstellen
      ((stammdatenReadRepository.getById(depotlieferungAboMapping, id) map { abo =>
        DepotlieferungAboModify
        abo.kundeId
      }) orElse (stammdatenReadRepository.getById(heimlieferungAboMapping, id) map { abo =>
        abo.kundeId
      }) orElse (stammdatenReadRepository.getById(postlieferungAboMapping, id) map { abo =>
        abo.kundeId
      })) map { kundeId =>
        //TODO: assemble text using gettext
        val title = "Guthaben angepasst: "
        val pendenzCreate = PendenzCreate(kundeId, meta.timestamp, Some(bemerkung), Erledigt, true)
        EntityInsertEvent[PendenzId, PendenzCreate](idFactory.newId(PendenzId.apply), pendenzCreate)
      }
    }
  }

  def createAboRechnungsPositionenAnzahlLieferungen(idFactory: IdFactory, meta: EventTransactionMetadata, aboRechnungCreate: AboRechnungsPositionBisAnzahlLieferungenCreate) = {
    DB readOnly { implicit session =>
      val abos: List[Abo] = stammdatenReadRepository.getByIds(depotlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(postlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(heimlieferungAboMapping, aboRechnungCreate.ids)

      val aboTypen: List[Abotyp] = stammdatenReadRepository.getByIds(abotypMapping, abos.map(_.abotypId))

      val abosWithAboTypen: List[(Abo, Abotyp)] = abos.map { abo =>
        aboTypen.find(_.id == abo.abotypId).map { abotyp => (abo, abotyp) }
      }.flatten

      val (events, failures) = abosWithAboTypen.map {
        case (abo, abotyp) =>

          // TODO check preisEinheit
          if (abotyp.preiseinheit != ProLieferung) {
            Failure(new InvalidStateException(s"Für den Abotyp dieses Abos (${abo.id}) kann keine Guthabenrechngsposition erstellt werden"))
          } else {
            // has to be refactored as soon as more modes are available
            val anzahlLieferungen = aboRechnungCreate.anzahlLieferungen
            if (anzahlLieferungen > 0) {
              val betrag = aboRechnungCreate.betrag.getOrElse(abotyp.preis * anzahlLieferungen)

              val rechnungsPosition = RechnungsPositionCreate(
                abo.kundeId,
                Some(abo.id),
                aboRechnungCreate.titel,
                Some(anzahlLieferungen),
                betrag,
                aboRechnungCreate.waehrung,
                RechnungsPositionStatus.Offen,
                RechnungsPositionTyp.Abo
              )

              Success(insertEntityEvent(idFactory, meta, rechnungsPosition, RechnungsPositionId.apply))
            } else {
              Failure(new InvalidStateException(s"Für das Abo mit der Id ${abo.id} wurde keine RechnungsPositionen erstellt. Anzahl Lieferungen 0"))
            }
          }
      } partition (_.isSuccess)

      if (events.isEmpty) {
        Failure(new InvalidStateException(s"Keine der RechnungsPositionen konnte erstellt werden"))
      } else {
        Success(events map (_.get))
      }
    }
  }

  def createAboRechnungsPositionenBisGuthaben(idFactory: IdFactory, meta: EventTransactionMetadata, aboRechnungCreate: AboRechnungsPositionBisGuthabenCreate) = {
    DB readOnly { implicit session =>
      val abos: List[Abo] = stammdatenReadRepository.getByIds(depotlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(postlieferungAboMapping, aboRechnungCreate.ids) :::
        stammdatenReadRepository.getByIds(heimlieferungAboMapping, aboRechnungCreate.ids)

      val aboTypen: List[Abotyp] = stammdatenReadRepository.getByIds(abotypMapping, abos.map(_.abotypId))

      val abosWithAboTypen: List[(Abo, Abotyp)] = abos.map { abo =>
        aboTypen.find(_.id == abo.abotypId).map { abotyp => (abo, abotyp) }
      }.flatten

      val (events, failures) = abosWithAboTypen.map {
        case (abo, abotyp) =>

          // TODO check preisEinheit
          if (abotyp.preiseinheit != ProLieferung) {
            Failure(new InvalidStateException(s"Für den Abotyp dieses Abos (${abo.id}) kann keine Guthabenrechngsposition erstellt werden"))
          } else {
            // has to be refactored as soon as more modes are available
            val anzahlLieferungen = math.max((aboRechnungCreate.bisGuthaben - abo.guthaben), 0)

            if (anzahlLieferungen > 0) {
              val betrag = abotyp.preis * anzahlLieferungen

              val rechnungsPosition = RechnungsPositionCreate(
                abo.kundeId,
                Some(abo.id),
                aboRechnungCreate.titel,
                Some(anzahlLieferungen),
                betrag,
                aboRechnungCreate.waehrung,
                RechnungsPositionStatus.Offen,
                RechnungsPositionTyp.Abo
              )

              Success(insertEntityEvent(idFactory, meta, rechnungsPosition, RechnungsPositionId.apply))
            } else {
              Failure(new InvalidStateException(s"Für das Abo mit der Id ${abo.id} wurde keine Rechnungsposition erstellt. Anzahl Lieferungen 0"))
            }
          }
      } partition (_.isSuccess)

      if (events.isEmpty) {
        Failure(new InvalidStateException(s"Keine der Rechnungspositionen konnte erstellt werden"))
      } else {
        Success(events map (_.get))
      }
    }
  }

  def sendEinladung(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: KundeId, personId: PersonId) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.email map { email =>
          Success(Seq(DefaultResultingEvent(factory => EinladungGesendetEvent(factory.newMetadata(), EinladungCreate(
            idFactory.newId(EinladungId.apply),
            personId,
            UUID.randomUUID.toString,
            DateTime.now.plusDays(90),
            None
          )))))
        } getOrElse {
          Failure(new InvalidStateException(s"Dieser Person kann keine Einladung gesendet werden da sie keine Emailadresse besitzt."))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  def sendPasswortReset(idFactory: IdFactory, meta: EventTransactionMetadata, personId: PersonId) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.email map { email =>
          Success(Seq(DefaultResultingEvent(factory => PasswortResetGesendetEvent(factory.newMetadata(), EinladungCreate(
            idFactory.newId(EinladungId.apply),
            personId,
            UUID.randomUUID.toString,
            DateTime.now.plusMinutes(120),
            None
          )))))
        } getOrElse {
          Failure(new InvalidStateException(s"Dieser Person kann keine Einladung gesendet werden da sie keine Emailadresse besitzt."))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  def changeRolle(idFactory: IdFactory, meta: EventTransactionMetadata, kundeId: KundeId, personId: PersonId, rolle: Rolle) = {
    DB readOnly { implicit session =>
      stammdatenReadRepository.getById(personMapping, personId) map { person =>
        person.rolle map { existingRolle =>
          if (existingRolle != rolle) {
            Success(Seq(DefaultResultingEvent(factory => RolleGewechseltEvent(factory.newMetadata(), kundeId, personId, rolle))))
          } else {
            Failure(new InvalidStateException(s"Die Person mit der Id: $personId hat bereits die Rolle: $rolle."))
          }
        } getOrElse {
          Success(Seq(DefaultResultingEvent(factory => RolleGewechseltEvent(factory.newMetadata(), kundeId, personId, rolle))))
        }
      } getOrElse {
        Failure(new InvalidStateException(s"Person wurde nicht gefunden."))
      }
    }
  }

  private def getCreateAuslieferungHeimEvent(lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession): Seq[ResultingEvent] = {
    val lieferungen = stammdatenReadRepository.getLieferungen(lieferplanung.id)

    //handle Tourenlieferungen: Group all entries with the same TourId on the same Date
    val vertriebsartenDaten = (lieferungen flatMap { lieferung =>
      stammdatenReadRepository.getVertriebsarten(lieferung.vertriebId) collect {
        case h: HeimlieferungDetail =>
          stammdatenReadRepository.getById(tourMapping, h.tourId) map { tour =>
            (h.tourId, tour.name, lieferung.datum) -> h.id
          }
      }
    }).flatten.groupBy(_._1).mapValues(_ map { _._2 })

    (vertriebsartenDaten flatMap {
      case ((tourId, tourName, lieferdatum), vertriebsartIds) => {
        //create auslieferungen
        if (!isAuslieferungExistingHeim(lieferdatum, tourId)) {
          val koerbe = stammdatenReadRepository.getKoerbe(lieferdatum, vertriebsartIds, WirdGeliefert)
          if (!koerbe.isEmpty) {
            val tourAuslieferung = createTourAuslieferungHeim(lieferdatum, tourId, tourName, koerbe.size)
            val updates = koerbe map { korb =>
              val tourlieferung = stammdatenReadRepository.getById[Tourlieferung, AboId](tourlieferungMapping, korb.aboId)
              val copy = korb.copy(auslieferungId = Some(tourAuslieferung.id), sort = tourlieferung flatMap (_.sort))
              EntityUpdateEvent(copy.id, copy)
            }
            EntityInsertEvent(tourAuslieferung.id, tourAuslieferung) :: updates
          } else { Nil }
        } else { Nil }
      }
    }).toSeq
  }

  private def getCreateDepotAuslieferungAndPostAusliferungEvent(lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession): Seq[ResultingEvent] = {

    val lieferungen = stammdatenReadRepository.getLieferungen(lieferplanung.id)

    val updates1 = handleLieferplanungAbgeschlossen(lieferungen)
    val updates2 = recalculateValuesForLieferplanungAbgeschlossen(lieferungen)
    val updates3 = updateSammelbestellungStatus(lieferungen, lieferplanung)
    updates1 ::: updates2 ::: updates3
  }

  private def handleLieferplanungAbgeschlossen(lieferungen: List[Lieferung])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {

    //handle Depot- and Postlieferungen: Group all entries with the same VertriebId on the same Date
    val vertriebeDaten = lieferungen.map(l => (l.vertriebId, l.datum)).distinct
    (vertriebeDaten map {
      case (vertriebId, lieferungDatum) => {
        logger.debug(s"handleLieferplanungAbgeschlossen (Depot & Post): ${vertriebId}:${lieferungDatum}.")
        //create auslieferungen
        val auslieferungL = stammdatenReadRepository.getVertriebsarten(vertriebId) map { vertriebsart =>
          getAuslieferungDepotPost(lieferungDatum, vertriebsart) match {
            case None => {
              logger.debug(s"createNewAuslieferung for: ${lieferungDatum}:${vertriebsart}.")
              val koerbe = stammdatenReadRepository.getKoerbe(lieferungDatum, vertriebsart.id, WirdGeliefert)
              if (!koerbe.isEmpty) {
                createAuslieferungDepotPost(lieferungDatum, vertriebsart, koerbe.size) map { newAuslieferung =>
                  val updates = koerbe map {
                    korb =>
                      EntityUpdateEvent(korb.id, KorbAuslieferungModify(newAuslieferung.id))
                  }
                  EntityInsertEvent(newAuslieferung.id, newAuslieferung) :: updates
                } getOrElse (Nil)
              } else {
                Nil
              }
            }
            case Some(auslieferung) => {
              val koerbe = stammdatenReadRepository.getKoerbe(lieferungDatum, vertriebsart.id, WirdGeliefert)
              koerbe map {
                korb =>
                  EntityUpdateEvent(korb.id, KorbAuslieferungModify(auslieferung.id))
              }
            }
          }
        }
        auslieferungL.flatten
      }
    }).flatten
  }

  private def recalculateValuesForLieferplanungAbgeschlossen(lieferungen: List[Lieferung])(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {
    //calculate new values
    (lieferungen flatMap { lieferung =>
      //calculate total of lieferung
      val total = stammdatenReadRepository.getLieferpositionenByLieferung(lieferung.id).map(_.preis.getOrElse(0.asInstanceOf[BigDecimal])).sum
      val lieferungCopy = lieferung.copy(preisTotal = total, status = Abgeschlossen)
      val lieferungModifyCopy = LieferungAbgeschlossenModify(Abgeschlossen, total)

      //update durchschnittspreis
      val updates = (stammdatenReadRepository.getProjekt flatMap { projekt =>
        stammdatenReadRepository.getVertrieb(lieferung.vertriebId) map { vertrieb =>
          val gjKey = projekt.geschaftsjahr.key(lieferung.datum.toLocalDate)

          val lieferungen = vertrieb.anzahlLieferungen.get(gjKey).getOrElse(0)
          val durchschnittspreis: BigDecimal = vertrieb.durchschnittspreis.get(gjKey).getOrElse(0)

          val neuerDurchschnittspreis = calcDurchschnittspreis(durchschnittspreis, lieferungen, total)
          val vertriebCopy = vertrieb.copy(
            anzahlLieferungen = vertrieb.anzahlLieferungen.updated(gjKey, lieferungen + 1),
            durchschnittspreis = vertrieb.durchschnittspreis.updated(gjKey, neuerDurchschnittspreis)
          )
          val vertriebModifyCopy = VertriebRecalculationsModify(vertrieb.anzahlLieferungen, vertrieb.durchschnittspreis)
          EntityUpdateEvent(vertrieb.id, vertriebModifyCopy) :: Nil
        }
      }).getOrElse(Nil)

      EntityUpdateEvent(lieferungCopy.id, lieferungModifyCopy) :: updates
    })
  }

  private def updateSammelbestellungStatus(lieferungen: List[Lieferung], lieferplanung: Lieferplanung)(implicit personId: PersonId, session: DBSession): List[ResultingEvent] = {

    (stammdatenReadRepository.getSammelbestellungen(lieferplanung.id) map {
      sammelbestellung =>
        if (Offen == sammelbestellung.status) {
          val sammelbestellungCopy = sammelbestellung.copy(status = Abgeschlossen)
          val sammelbestellungStatusModifyCopy = SammelbestellungStatusModify(sammelbestellungCopy.status)

          Seq(EntityUpdateEvent(sammelbestellungCopy.id, sammelbestellungStatusModifyCopy))
        } else { Nil }
    }).filter(_.nonEmpty).flatten
  }

  private def createAuslieferungDepotPost(lieferungDatum: DateTime, vertriebsart: VertriebsartDetail, anzahlKoerbe: Int)(implicit personId: PersonId): Option[Auslieferung] = {
    val auslieferungId = AuslieferungId(IdUtil.positiveRandomId)

    vertriebsart match {
      case d: DepotlieferungDetail =>
        val result = DepotAuslieferung(
          auslieferungId,
          Erfasst,
          d.depotId,
          d.depot.name,
          lieferungDatum,
          anzahlKoerbe,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )
        Some(result)

      case p: PostlieferungDetail =>
        val result = PostAuslieferung(
          auslieferungId,
          Erfasst,
          lieferungDatum,
          anzahlKoerbe,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )
        Some(result)

      case _ =>
        None
    }
  }

  private def getAuslieferungDepotPost(datum: DateTime, vertriebsart: VertriebsartDetail)(implicit session: DBSession): Option[Auslieferung] = {
    vertriebsart match {
      case d: DepotlieferungDetail =>
        stammdatenReadRepository.getDepotAuslieferung(d.depotId, datum)
      case p: PostlieferungDetail =>
        stammdatenReadRepository.getPostAuslieferung(datum)
      case _ =>
        None
    }
  }

  private def isAuslieferungExistingHeim(datum: DateTime, tourId: TourId)(implicit session: DBSession): Boolean = {
    stammdatenReadRepository.getTourAuslieferung(tourId, datum).isDefined
  }

  private def createTourAuslieferungHeim(lieferungDatum: DateTime, tourId: TourId, tourName: String, anzahlKoerbe: Int)(implicit personId: PersonId): TourAuslieferung = {
    val auslieferungId = AuslieferungId(IdUtil.positiveRandomId)
    TourAuslieferung(
      auslieferungId,
      Erfasst,
      tourId,
      tourName,
      lieferungDatum,
      anzahlKoerbe,
      DateTime.now,
      personId,
      DateTime.now,
      personId
    )
  }

  private def getDistinctSammelbestellungModifyByLieferplan(lieferplanungId: LieferplanungId)(implicit session: DBSession): Set[SammelbestellungModify] = {
    stammdatenReadRepository.getLieferpositionenByLieferplan(lieferplanungId).map { lieferposition =>
      stammdatenReadRepository.getById(lieferungMapping, lieferposition.lieferungId).map { lieferung =>
        SammelbestellungModify(lieferposition.produzentId, lieferplanungId, lieferung.datum)
      }
    }.flatten.toSet
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCommandHandler
    with DefaultStammdatenReadRepositorySyncComponent {
}
