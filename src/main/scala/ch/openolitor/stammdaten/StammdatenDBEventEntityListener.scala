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

import akka.actor._

import spray.json._
import scalikejdbc._
import ch.openolitor.core.models._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.db._
import ch.openolitor.core.SystemConfig
import ch.openolitor.buchhaltung.models._
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import BigDecimal.RoundingMode._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object StammdatenDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenDBEventEntityListener], sysConfig, system)
}

class DefaultStammdatenDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenDBEventEntityListener(sysConfig) with DefaultStammdatenUpdateRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class StammdatenDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging
    with StammdatenDBMappings
    with ConnectionPoolContextAware
    with KorbStatusHandler
    with AboAktivChangeHandler
    with LieferungDurchschnittspreisHandler {
  this: StammdatenUpdateRepositoryComponent =>
  import SystemEvents._

  val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
    context.system.eventStream.subscribe(self, classOf[SystemEvent])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    context.system.eventStream.unsubscribe(self, classOf[SystemEvent])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityModified(personId, entity: Abotyp, orig: Abotyp) if entity.name != orig.name =>
      handleAbotypModify(orig, entity)(personId)
    case e @ EntityCreated(personId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboCreated(entity)(personId)
      handleAboCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboDeleted(entity)(personId)
      handleAboDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: DepotlieferungAbo, orig: DepotlieferungAbo) if entity.depotId != orig.depotId =>
      handleDepotlieferungAboDepotChanged(entity, orig.depotId, entity.depotId)(personId)
      handleAboModified(orig, entity)(personId)
    case e @ EntityCreated(personId, entity: HeimlieferungAbo) =>
      handleHeimlieferungAboCreated(entity)(personId)
      handleAboCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: HeimlieferungAbo) =>
      handleHeimlieferungAboDeleted(entity)(personId)
      handleAboDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: HeimlieferungAbo, orig: HeimlieferungAbo) if entity.tourId != orig.tourId =>
      handleHeimlieferungAboTourChanged(entity, orig.tourId, entity.tourId)(personId)
      handleAboModified(orig, entity)(personId)
    case e @ EntityModified(personId, entity: HeimlieferungAbo, orig: HeimlieferungAbo) =>
      handleAboModified(orig, entity)(personId)
    case e @ EntityModified(personId, entity: PostlieferungAbo, orig: PostlieferungAbo) if entity.vertriebId != orig.vertriebId =>
      handleAboModified(orig, entity)(personId)

    case e @ EntityCreated(personId, entity: ZusatzAbo) => handleZusatzAboCreated(entity)(personId)
    case e @ EntityModified(personId, entity: ZusatzAbo, orig: ZusatzAbo) => handleZusatzAboModified(orig, entity)(personId)
    case e @ EntityDeleted(personId, entity: ZusatzAbo) => handleZusatzAboDeleted(entity)(personId)

    case e @ EntityCreated(personId, entity: Abo) => handleAboCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Abo) => handleAboDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Abo, orig: Abo) => handleAboModified(orig, entity)(personId)
    case e @ EntityCreated(personId, entity: Abwesenheit) => handleAbwesenheitCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Abwesenheit) => handleAbwesenheitDeleted(entity)(personId)

    case e @ EntityCreated(personId, entity: Kunde) => handleKundeCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Kunde) => handleKundeDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Kunde, orig: Kunde) => handleKundeModified(entity, orig)(personId)

    case e @ EntityDeleted(personId, entity: Person) => handlePersonDeleted(entity)(personId)

    case e @ EntityCreated(personId, entity: Pendenz) => handlePendenzCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Pendenz) => handlePendenzDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Pendenz, orig: Pendenz) => handlePendenzModified(entity, orig)(personId)

    case e @ EntityCreated(personId, entity: RechnungsPosition) => handleRechnungsPositionCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: RechnungsPosition) => handleRechnungsPositionDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: RechnungsPosition, orig: RechnungsPosition) if (orig.status != RechnungsPositionStatus.Bezahlt && entity.status == RechnungsPositionStatus.Bezahlt) =>
      handleRechnungsPositionBezahlt(entity, orig)(personId)

    case e @ EntityCreated(personId, entity: Lieferplanung) => handleLieferplanungCreated(entity)(personId)

    case e @ EntityDeleted(personId, entity: Lieferplanung) => handleLieferplanungDeleted(entity)(personId)
    case e @ PersonLoggedIn(personId, timestamp) => handlePersonLoggedIn(personId, timestamp)

    case e @ EntityModified(personId, entity: Lieferplanung, orig: Lieferplanung) =>
      handleLieferplanungModified(entity, orig)(personId)
    case e @ EntityModified(personId, entity: Lieferung, orig: Lieferung) //Die Lieferung wird von der Lieferplanung entfernt
    if (orig.lieferplanungId.isEmpty && entity.lieferplanungId.isDefined) =>
      handleLieferplanungLieferungenChanged(entity.lieferplanungId.get)(personId)
    case e @ EntityModified(personId, entity: Lieferung, orig: Lieferung) //Die Lieferung wird an eine Lieferplanung angehängt
    if (orig.lieferplanungId.isDefined && entity.lieferplanungId.isEmpty) => handleLieferplanungLieferungenChanged(orig.lieferplanungId.get)(personId)

    case e @ EntityModified(personId, entity: Lieferung, orig: Lieferung) if (entity.lieferplanungId.isDefined) => handleLieferungChanged(entity, orig)(personId)

    case e @ EntityModified(personId, entity: Vertriebsart, orig: Vertriebsart) => handleVertriebsartModified(entity, orig)(personId)

    case e @ EntityModified(personId, entity: Auslieferung, orig: Auslieferung) if (orig.status == Erfasst && entity.status == Ausgeliefert) =>
      handleAuslieferungAusgeliefert(entity)(personId)

    case e @ EntityModified(userId, entity: Depot, orig: Depot) => handleDepotModified(entity, orig)(userId)
    case e @ EntityModified(userId, entity: Korb, orig: Korb) if entity.status != orig.status => handleKorbStatusChanged(entity, orig.status)(userId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleAbotypModify(orig: Abotyp, entity: Abotyp)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getAbosByAbotyp(entity.id) map { abo =>
        stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](abo.id) { abo =>
          Map(depotlieferungAboMapping.column.abotypName -> entity.name)
        }

        stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](abo.id) { abo =>
          Map(heimlieferungAboMapping.column.abotypName -> entity.name)
        }

        stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](abo.id) { abo =>
          Map(postlieferungAboMapping.column.abotypName -> entity.name)
        }
      }
    }
  }

  def handleVertriebsartModified(vertriebsart: Vertriebsart, orig: Vertriebsart)(implicit personId: PersonId) = {
    //update Beschrieb on Vertrieb
  }

  private def calculateAboAktivCreate(abo: Abo): Int = {
    if (abo.aktiv) 1 else 0
  }

  def handleDepotlieferungAboCreated(abo: DepotlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Depot, DepotId](abo.depotId) { depot =>
        log.debug(s"Add abonnent to depot:${depot.id}")
        Map(
          depotMapping.column.anzahlAbonnenten -> (depot.anzahlAbonnenten + 1),
          depotMapping.column.anzahlAbonnentenAktiv -> (depot.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
        )
      }
    }
  }

  def handleDepotlieferungAboDeleted(abo: DepotlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Depot, DepotId](abo.depotId) { depot =>
        log.debug(s"Remove abonnent from depot:${depot.id}")
        Map(
          depotMapping.column.anzahlAbonnenten -> (depot.anzahlAbonnenten - 1),
          depotMapping.column.anzahlAbonnentenAktiv -> (depot.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
        )
      }
    }
  }

  def handleDepotlieferungAboDepotChanged(abo: DepotlieferungAbo, from: DepotId, to: DepotId)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Depot, DepotId](from) { depot =>
        log.debug(s"Remove abonnent from depot:${depot.id}")
        Map(
          depotMapping.column.anzahlAbonnenten -> (depot.anzahlAbonnenten - 1),
          depotMapping.column.anzahlAbonnentenAktiv -> (depot.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
        )
      }

      stammdatenUpdateRepository.modifyEntity[Depot, DepotId](to) { depot =>
        log.debug(s"Add abonnent to depot:${depot.id}")
        Map(
          depotMapping.column.anzahlAbonnenten -> (depot.anzahlAbonnenten + 1),
          depotMapping.column.anzahlAbonnentenAktiv -> (depot.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
        )
      }
    }
  }

  def handleHeimlieferungAboCreated(entity: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Tour, TourId](entity.tourId) { tour =>
        log.debug(s"Add abonnent to tour:${tour.id}")
        Map(
          tourMapping.column.anzahlAbonnenten -> (tour.anzahlAbonnenten + 1),
          tourMapping.column.anzahlAbonnentenAktiv -> (tour.anzahlAbonnentenAktiv + calculateAboAktivCreate(entity))
        )
      }
    }
  }

  def handleHeimlieferungAboDeleted(abo: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Tour, TourId](abo.tourId) { tour =>
        log.debug(s"Remove abonnent from tour:${tour.id}")
        Map(
          tourMapping.column.anzahlAbonnenten -> (tour.anzahlAbonnenten - 1),
          tourMapping.column.anzahlAbonnentenAktiv -> (tour.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
        )
      }
    }
  }

  def handleHeimlieferungAboTourChanged(abo: HeimlieferungAbo, from: TourId, to: TourId)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Tour, TourId](from) { tour =>
        log.debug(s"Remove abonnent from tour:${tour.id}")
        Map(
          tourMapping.column.anzahlAbonnenten -> (tour.anzahlAbonnenten - 1),
          tourMapping.column.anzahlAbonnentenAktiv -> (tour.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
        )
      }

      stammdatenUpdateRepository.modifyEntity[Tour, TourId](to) { tour =>
        log.debug(s"Add abonnent to tour:${tour.id}")
        Map(
          tourMapping.column.anzahlAbonnenten -> (tour.anzahlAbonnenten + 1),
          tourMapping.column.anzahlAbonnentenAktiv -> (tour.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
        )
      }
    }
  }

  def handleAboCreated(abo: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modAboCount = calculateAboAktivCreate(abo)
      stammdatenUpdateRepository.modifyEntity[Abotyp, AbotypId](abo.abotypId) { abotyp =>
        log.debug(s"Add abonnent to abotyp:${abotyp.id}")
        Map(
          abotypMapping.column.anzahlAbonnenten -> (abotyp.anzahlAbonnenten + 1),
          abotypMapping.column.anzahlAbonnentenAktiv -> (abotyp.anzahlAbonnentenAktiv + modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](abo.kundeId) { kunde =>
        log.debug(s"Add abonnent to kunde:${kunde.id}")
        Map(
          kundeMapping.column.anzahlAbos -> (kunde.anzahlAbos + 1),
          kundeMapping.column.anzahlAbosAktiv -> (kunde.anzahlAbosAktiv + modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
        log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
        Map(
          vertriebMapping.column.anzahlAbos -> (vertrieb.anzahlAbos + 1),
          vertriebMapping.column.anzahlAbosAktiv -> (vertrieb.anzahlAbosAktiv + modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        Map(
          depotlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos + 1),
          depotlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        Map(
          heimlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos + 1),
          heimlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        Map(
          postlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos + 1),
          postlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + modAboCount)
        )
      }
    }
  }

  def handleAboModified(from: Abo, to: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (from.aktiv != to.aktiv) {
        handleAboAktivChange(to, if (to.aktiv) 1 else -1)
      }

      if (from.start != to.start) {
        stammdatenUpdateRepository.getZusatzAbos(from.id) map { zusatzabo =>
          if ((zusatzabo.start compareTo to.start) < 0) {
            stammdatenUpdateRepository.modifyEntity[ZusatzAbo, AboId](zusatzabo.id) { z =>
              log.debug(s"modify the start date of the zusatzabo :${z.id}")
              Map(
                zusatzAboMapping.column.start -> to.start
              )
            }
          }
        }
      }

      (from.ende, to.ende) match {
        case (None, Some(toEnde)) => handleZusatzAboEndDateModification(toEnde, from)
        case (Some(fromEnde), Some(toEnde)) => if (fromEnde != toEnde) handleZusatzAboEndDateModification(toEnde, from)
        case (_, _) =>
      }

      if (from.vertriebId != to.vertriebId) {
        val modAboCount = calculateAboAktivCreate(to)
        stammdatenUpdateRepository.modifyEntity[Vertrieb, VertriebId](from.vertriebId) { vertrieb =>
          log.debug(s"Remove abonnent from vertrieb:${vertrieb.id}")
          Map(
            vertriebMapping.column.anzahlAbos -> (vertrieb.anzahlAbos - 1),
            vertriebMapping.column.anzahlAbosAktiv -> (vertrieb.anzahlAbosAktiv - modAboCount)
          )
        }

        stammdatenUpdateRepository.modifyEntity[Vertrieb, VertriebId](to.vertriebId) { vertrieb =>
          log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
          Map(
            vertriebMapping.column.anzahlAbos -> (vertrieb.anzahlAbos + 1),
            vertriebMapping.column.anzahlAbosAktiv -> (vertrieb.anzahlAbosAktiv + modAboCount)
          )
        }
      }
    }
  }

  def handleZusatzAboEndDateModification(toEnde: LocalDate, from: Abo)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenUpdateRepository.getZusatzAbos(from.id) map { zusatzabo =>
      zusatzabo.ende match {
        case Some(zusatzaboEnde) => {
          if ((zusatzaboEnde compareTo toEnde) > 0) {
            stammdatenUpdateRepository.modifyEntity[ZusatzAbo, AboId](zusatzabo.id) { z =>
              log.debug(s"modify the end date of the zusatzabo :${z.id}")
              Map(
                zusatzAboMapping.column.ende -> toEnde
              )
            }
          }
        }
        case _ => stammdatenUpdateRepository.modifyEntity[ZusatzAbo, AboId](zusatzabo.id) { z =>
          log.debug(s"modify the end date of the zusatzabo :${z.id}")
          Map(
            zusatzAboMapping.column.ende -> toEnde
          )
        }
      }
    }
  }

  def handleAboDeleted(abo: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modAboCount = calculateAboAktivCreate(abo)
      stammdatenUpdateRepository.modifyEntity[Abotyp, AbotypId](abo.abotypId) { abotyp =>
        log.debug(s"Remove abonnent from abotyp:${abotyp.id}")
        Map(
          abotypMapping.column.anzahlAbonnenten -> (abotyp.anzahlAbonnenten - 1),
          abotypMapping.column.anzahlAbonnentenAktiv -> (abotyp.anzahlAbonnentenAktiv - modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](abo.kundeId) { kunde =>
        log.debug(s"Remove abonnent from kunde:${kunde.id}")
        Map(
          kundeMapping.column.anzahlAbos -> (kunde.anzahlAbos - 1),
          kundeMapping.column.anzahlAbosAktiv -> (kunde.anzahlAbosAktiv - modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
        log.debug(s"Remove abonnent from vertrieb:${vertrieb.id}")
        Map(
          vertriebMapping.column.anzahlAbos -> (vertrieb.anzahlAbos - 1),
          vertriebMapping.column.anzahlAbosAktiv -> (vertrieb.anzahlAbosAktiv - modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        Map(
          depotlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos - 1),
          depotlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv - modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        Map(
          heimlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos - 1),
          heimlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv - modAboCount)
        )
      }

      stammdatenUpdateRepository.modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        Map(
          postlieferungMapping.column.anzahlAbos -> (vertriebsart.anzahlAbos - 1),
          postlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv - modAboCount)
        )
      }
    }
  }

  def handleZusatzAboCreated(zusatzAbo: ZusatzAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modZusatzAboCount = calculateAboAktivCreate(zusatzAbo)
      stammdatenUpdateRepository.modifyEntity[ZusatzAbotyp, AbotypId](zusatzAbo.abotypId) { zusatzAbotyp =>
        log.debug(s"Add abonnent to zusatzabotyp:${zusatzAbotyp.id}")
        Map(
          zusatzAbotypMapping.column.anzahlAbonnenten -> (zusatzAbotyp.anzahlAbonnenten + 1),
          zusatzAbotypMapping.column.anzahlAbonnentenAktiv -> (zusatzAbotyp.anzahlAbonnentenAktiv + modZusatzAboCount)
        )
      }

      stammdatenUpdateRepository.updateHauptAboAddZusatzabo(zusatzAbo)
    }
  }

  def handleZusatzAboModified(from: ZusatzAbo, to: ZusatzAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (from.aktiv != to.aktiv) {
        val change = if (to.aktiv) 1 else -1
        stammdatenUpdateRepository.modifyEntity[ZusatzAbotyp, AbotypId](to.abotypId) { zusatzAbotyp =>
          Map(zusatzAbotypMapping.column.anzahlAbonnentenAktiv -> (zusatzAbotyp.anzahlAbonnentenAktiv + change))
        }
      }

      if (from.abotypName != to.abotypName) {
        stammdatenUpdateRepository.updateHauptAboWithZusatzabo(to.hauptAboId, to, from)
      }
    }
  }

  def handleZusatzAboDeleted(zusatzAbo: ZusatzAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modZusatzAboCount = calculateAboAktivCreate(zusatzAbo)
      stammdatenUpdateRepository.modifyEntity[ZusatzAbotyp, AbotypId](zusatzAbo.abotypId) { zusatzAbotyp =>
        log.debug(s"Remove abonnent from zusatzabotyp:${zusatzAbotyp.id}")
        Map(
          zusatzAbotypMapping.column.anzahlAbonnenten -> (zusatzAbotyp.anzahlAbonnenten - 1),
          zusatzAbotypMapping.column.anzahlAbonnentenAktiv -> (zusatzAbotyp.anzahlAbonnentenAktiv - modZusatzAboCount)
        )
      }

      stammdatenUpdateRepository.updateHauptAboRemoveZusatzabo(zusatzAbo)
    }
  }

  def handleKundeModified(kunde: Kunde, orig: Kunde)(implicit personId: PersonId) = {
    //compare typen
    //find removed typen
    val removed = orig.typen -- kunde.typen

    //tag typen which where added
    val added = kunde.typen -- orig.typen

    log.debug(s"Kunde ${kunde.bezeichnung} modified, handle CustomKundentypen. Orig: ${orig.typen} -> modified: ${kunde.typen}. Removed typen:${removed}, added typen:${added}")

    handleKundentypenChanged(removed, added)

    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getPendenzen(kunde.id) map { pendenz =>
        log.debug(s"Modify Kundenbezeichnung on Pendenz to : ${kunde.bezeichnung}.")
        stammdatenUpdateRepository.updateEntity[Pendenz, PendenzId](pendenz.id)(pendenzMapping.column.kundeBezeichnung -> kunde.bezeichnung)
      }

      updateTourlieferungenByKunde(kunde)
    }

  }

  def handleKundeDeleted(kunde: Kunde)(implicit personId: PersonId) = {
    handleKundentypenChanged(kunde.typen, Set())
  }

  def handleKundeCreated(kunde: Kunde)(implicit personId: PersonId) = {
    handleKundentypenChanged(Set(), kunde.typen)
  }

  def handlePersonDeleted(person: Person)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val personen = stammdatenUpdateRepository.getPersonen(person.kundeId)
      if (personen.size == 1) {
        stammdatenUpdateRepository.updateEntity[Kunde, KundeId](person.kundeId)(kundeMapping.column.bezeichnung -> personen.head.fullName)
      }
      // Recalculate sort field on Persons
      personen.zipWithIndex.map {
        case (person, index) =>
          val sortValue = (index + 1)
          if (sortValue != person.sort) {
            logger.debug(s"Update sort for Person {person.id} {person.vorname} {person.name} to {sortValue}")
            stammdatenUpdateRepository.updateEntity[Person, PersonId](person.id)(personMapping.column.sort -> sortValue)
          }
      }
    }
  }

  private def handleAbwesenheitDeleted(abw: Abwesenheit)(implicit personId: PersonId): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getProjekt map { projekt =>
        val geschaeftsjahrKey = projekt.geschaftsjahr.key(abw.datum)

        stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          Map(depotlieferungAboMapping.column.anzahlAbwesenheiten -> (abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value)))
        }

        stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          Map(heimlieferungAboMapping.column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }

        stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          Map(postlieferungAboMapping.column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
      }

      stammdatenUpdateRepository.getAboDetailAusstehend(abw.aboId) match {
        case Some(abo) => {
          stammdatenUpdateRepository.getAbotypDetail(abo.abotypId) map { abotyp =>
            //re count because the might be another abwesenheit for the same date
            val newAbwesenheitCount = stammdatenUpdateRepository.countAbwesend(abw.aboId, abw.datum)
            val status = calculateKorbStatus(newAbwesenheitCount, abo.guthaben, abotyp.guthabenMindestbestand)
            stammdatenUpdateRepository.getKorb(abw.lieferungId, abw.aboId).toList ++
              stammdatenUpdateRepository.getZusatzAboKorb(abw.lieferungId, abw.aboId) map { korb =>
                val statusAlt = korb.status
                log.debug(s"Modify Korb-Status as Abwesenheit was deleted ${abw.id}, newCount:$newAbwesenheitCount, newStatus:${status}.")
                stammdatenUpdateRepository.updateEntity[Korb, KorbId](korb.id)(korbMapping.column.status -> status)
              }
          }
        }
        case None => log.error(s"There should be an abo with this id : ${abw.aboId}")
      }
    }
  }

  def handleAbwesenheitCreated(abw: Abwesenheit)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getProjekt map { projekt =>
        val geschaeftsjahrKey = projekt.geschaftsjahr.key(abw.datum)

        stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          Map(depotlieferungAboMapping.column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }

        stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          Map(heimlieferungAboMapping.column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }

        stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          Map(postlieferungAboMapping.column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
      }

      stammdatenUpdateRepository.getKorb(abw.lieferungId, abw.aboId) match {
        case Some(korb) => {
          log.debug(s"Modify Korb-Status as Abwesenheit was created : ${korb.id}: ${FaelltAusAbwesend}.")
          stammdatenUpdateRepository.updateEntity[Korb, KorbId](korb.id)(korbMapping.column.status -> FaelltAusAbwesend)
        }
        case None => log.debug(s"No Korb yet for Lieferung : ${abw.lieferungId} and Abotyp : ${abw.aboId}")
      }

      stammdatenUpdateRepository.getZusatzAboKorb(abw.lieferungId, abw.aboId).map { zusatzKorb =>
        stammdatenUpdateRepository.updateEntity[Korb, KorbId](zusatzKorb.id)(korbMapping.column.status -> FaelltAusAbwesend)
      }
    }
  }

  private def handleKorbStatusChanged(korb: Korb, statusAlt: KorbStatus)(implicit personId: PersonId): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>

      stammdatenUpdateRepository.modifyEntity[Lieferung, LieferungId](korb.lieferungId) { lieferung =>
        log.debug(s"Korb Status changed:${korb.aboId}/${korb.lieferungId}")

        val result = recalculateLieferungCounts(lieferung, korb.status, statusAlt)

        Map(
          lieferungMapping.column.anzahlKoerbeZuLiefern -> result.anzahlKoerbeZuLiefern,
          lieferungMapping.column.anzahlAbwesenheiten -> result.anzahlAbwesenheiten,
          lieferungMapping.column.anzahlSaldoZuTief -> result.anzahlSaldoZuTief
        )
        // TODO who is changing KorbStatus?
        // kann the handle KorbStatusChanged be removed and the recaculate be done?
      }
    }
  }

  private def recalculateLieferungCounts(lieferung: Lieferung, korbStatusNeu: KorbStatus, korbStatusAlt: KorbStatus)(implicit personId: PersonId, session: DBSession) = {
    val zuLiefernDiff = korbStatusNeu match {
      case WirdGeliefert => 1
      case Geliefert if korbStatusAlt == WirdGeliefert => 0 // TODO introduce additional counter for delivered baskets
      case _ if korbStatusAlt == WirdGeliefert => -1
      case _ => 0
    }
    val abwDiff = korbStatusNeu match {
      case FaelltAusAbwesend => 1
      case _ if korbStatusAlt == FaelltAusAbwesend => -1
      case _ => 0
    }
    val saldoDiff = korbStatusNeu match {
      case FaelltAusSaldoZuTief => 1
      case _ if korbStatusAlt == FaelltAusSaldoZuTief => -1
      case _ => 0
    }

    val copy = lieferung.copy(
      anzahlKoerbeZuLiefern = lieferung.anzahlKoerbeZuLiefern + zuLiefernDiff,
      anzahlAbwesenheiten = lieferung.anzahlAbwesenheiten + abwDiff,
      anzahlSaldoZuTief = lieferung.anzahlSaldoZuTief + saldoDiff
    )
    log.debug(s"Recalculate Lieferung as Korb-Status: was modified : ${lieferung.id} status form ${korbStatusAlt} to ${korbStatusNeu}. zu lieferung:$zuLiefernDiff, Abw: $abwDiff, Saldo: $saldoDiff\nfrom:$lieferung\nto:$copy")
    copy

  }

  def handlePendenzCreated(pendenz: Pendenz)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
        log.debug(s"Add pendenz count to kunde:${kunde.id}")
        Map(kundeMapping.column.anzahlPendenzen -> (kunde.anzahlPendenzen + 1))
      }
    }
  }

  def handlePendenzDeleted(pendenz: Pendenz)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
        log.debug(s"Remove pendenz count from kunde:${kunde.id}")
        Map(kundeMapping.column.anzahlPendenzen -> (kunde.anzahlPendenzen - 1))
      }
    }
  }

  def handlePendenzModified(pendenz: Pendenz, orig: Pendenz)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (pendenz.status == Erledigt && orig.status != Erledigt) {
        stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          Map(kundeMapping.column.anzahlPendenzen -> (kunde.anzahlPendenzen - 1))
        }
      } else if (pendenz.status != Erledigt && orig.status == Erledigt) {
        stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          Map(kundeMapping.column.anzahlPendenzen -> (kunde.anzahlPendenzen + 1))
        }
      }
    }
  }

  def handleKundentypenChanged(removed: Set[KundentypId], added: Set[KundentypId])(implicit personId: PersonId) = {
    // Only count if it is not a rename of a Kundentyp
    DB localTxPostPublish { implicit session => implicit publisher =>
      val kundetypen = stammdatenUpdateRepository.getCustomKundentypen
      val kundentypenSet: Set[KundentypId] = kundetypen.map(_.kundentyp).toSet
      val rename = removed.size == 1 &&
        added.size == 1 &&
        kundentypenSet.intersect(removed).size == 0

      if (!rename) {
        removed.map { kundetypId =>
          kundetypen.filter(kt => kt.kundentyp == kundetypId).headOption.map {
            case customKundentyp: CustomKundentyp =>
              log.debug(s"Reduce anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${customKundentyp.anzahlVerknuepfungen - 1}")
              stammdatenUpdateRepository.updateEntity[CustomKundentyp, CustomKundentypId](customKundentyp.id) {
                customKundentypMapping.column.anzahlVerknuepfungen -> (customKundentyp.anzahlVerknuepfungen - 1)
              }
          }
        }
        added.map { kundetypId =>
          kundetypen.filter(kt => kt.kundentyp == kundetypId).headOption.map {
            case customKundentyp: CustomKundentyp =>
              log.debug(s"Increment anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${customKundentyp.anzahlVerknuepfungen + 1}")
              stammdatenUpdateRepository.updateEntity[CustomKundentyp, CustomKundentypId](customKundentyp.id) {
                customKundentypMapping.column.anzahlVerknuepfungen -> (customKundentyp.anzahlVerknuepfungen + 1)
              }
          }
        }
      }
    }
  }

  def handleRechnungsPositionDeleted(rechnungsPosition: RechnungsPosition)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      for {
        aboId <- rechnungsPosition.aboId
        anzahl <- rechnungsPosition.anzahlLieferungen
      } yield {
        adjustGuthabenInRechnungsPosition(aboId, 0 - anzahl)
      }
    }
  }

  def handleRechnungsPositionCreated(rechnungsPosition: RechnungsPosition)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      for {
        aboId <- rechnungsPosition.aboId
        anzahl <- rechnungsPosition.anzahlLieferungen
      } yield {
        adjustGuthabenInRechnungsPosition(aboId, anzahl)
      }
    }
  }

  def handleRechnungsPositionBezahlt(rechnungsPosition: RechnungsPosition, orig: RechnungsPosition)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      for {
        aboId <- rechnungsPosition.aboId
        anzahlLieferungen <- rechnungsPosition.anzahlLieferungen
      } yield {
        stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](aboId) { abo =>
          Map(
            depotlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung - anzahlLieferungen),
            depotlieferungAboMapping.column.guthaben -> (abo.guthaben + anzahlLieferungen),
            depotlieferungAboMapping.column.guthabenVertraglich -> (abo.guthabenVertraglich map (_ - anzahlLieferungen) orElse (None))
          )
        }

        stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](aboId) { abo =>
          Map(
            postlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung - anzahlLieferungen),
            postlieferungAboMapping.column.guthaben -> (abo.guthaben + anzahlLieferungen),
            postlieferungAboMapping.column.guthabenVertraglich -> (abo.guthabenVertraglich map (_ - anzahlLieferungen) orElse (None))
          )
        }

        stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](aboId) { abo =>
          Map(
            heimlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung - anzahlLieferungen),
            heimlieferungAboMapping.column.guthaben -> (abo.guthaben + anzahlLieferungen),
            heimlieferungAboMapping.column.guthabenVertraglich -> (abo.guthabenVertraglich map (_ - anzahlLieferungen) orElse (None))
          )
        }
      }
    }
  }

  def handleRechnungsPositionGuthabenModified(rechnungsPosition: RechnungsPosition, orig: RechnungsPosition)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      for {
        aboId <- rechnungsPosition.aboId
        anzahlLieferungen <- rechnungsPosition.anzahlLieferungen
      } yield {
        adjustGuthabenInRechnungsPosition(aboId, anzahlLieferungen - orig.anzahlLieferungen.getOrElse(0))
      }

    }
  }

  private def adjustGuthabenInRechnungsPosition(aboId: AboId, diff: Int)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](aboId) { abo =>
      Map(depotlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung + diff))
    }

    stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](aboId) { abo =>
      Map(postlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung + diff))
    }

    stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](aboId) { abo =>
      Map(heimlieferungAboMapping.column.guthabenInRechnung -> (abo.guthabenInRechnung + diff))
    }
  }

  def handleLieferplanungCreated(lieferplanung: Lieferplanung)(implicit personId: PersonId) = {
  }

  def handleLieferplanungDeleted(lieferplanung: Lieferplanung)(implicit personId: PersonId) = {

  }

  def handleAuslieferungAusgeliefert(entity: Auslieferung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getKoerbe(entity.id) map { korb =>
        stammdatenUpdateRepository.updateEntity[Korb, KorbId](korb.id)(korbMapping.column.status -> Geliefert)

        stammdatenUpdateRepository.getProjekt map { projekt =>
          val geschaeftsjahrKey = projekt.geschaftsjahr.key(entity.datum.toLocalDate)

          stammdatenUpdateRepository.modifyEntity[DepotlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            Map(
              depotlieferungAboMapping.column.guthaben -> (korb.guthabenVorLieferung - 1),
              depotlieferungAboMapping.column.letzteLieferung -> getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              depotlieferungAboMapping.column.anzahlLieferungen -> abo.anzahlLieferungen.updated(geschaeftsjahrKey, value)
            )
          }

          stammdatenUpdateRepository.modifyEntity[HeimlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            Map(
              heimlieferungAboMapping.column.guthaben -> (korb.guthabenVorLieferung - 1),
              heimlieferungAboMapping.column.letzteLieferung -> getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              heimlieferungAboMapping.column.anzahlLieferungen -> abo.anzahlLieferungen.updated(geschaeftsjahrKey, value)
            )
          }

          stammdatenUpdateRepository.modifyEntity[PostlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            Map(
              postlieferungAboMapping.column.guthaben -> (korb.guthabenVorLieferung - 1),
              postlieferungAboMapping.column.letzteLieferung -> getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              postlieferungAboMapping.column.anzahlLieferungen -> abo.anzahlLieferungen.updated(geschaeftsjahrKey, value)
            )
          }

          stammdatenUpdateRepository.modifyEntity[ZusatzAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            Map(
              zusatzAboMapping.column.letzteLieferung -> getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              zusatzAboMapping.column.anzahlLieferungen -> abo.anzahlLieferungen.updated(geschaeftsjahrKey, value)
            )
          }
        }
      }
    }
  }

  private def updateAbotypOnAusgeliefert(abotypId: AbotypId, letzteLieferung: DateTime)(implicit personId: PersonId, dbSession: DBSession, publisher: EventPublisher) = {
    stammdatenUpdateRepository.modifyEntity[Abotyp, AbotypId](abotypId) { abotyp =>
      Map(abotypMapping.column.letzteLieferung -> getLatestDate(abotyp.letzteLieferung, Some(letzteLieferung)))
    }
  }

  private def getLatestDate(date1: Option[DateTime], date2: Option[DateTime]): Option[DateTime] = {
    if (date2 == None || (date1 != None && date1.get.isAfter(date2.get))) {
      date1
    } else {
      date2
    }
  }

  def handleDepotModified(depot: Depot, orig: Depot)(implicit personId: PersonId) = {
    logger.debug(s"handleDepotModified: depot:$depot  orig:$orig")
    if (depot.name != orig.name) {
      //Depot name was changed. Replace it in Abos
      DB localTxPostPublish { implicit session => implicit publisher =>
        stammdatenUpdateRepository.getDepotlieferungAbosByDepot(depot.id) map { abo =>
          stammdatenUpdateRepository.updateEntity[DepotlieferungAbo, AboId](abo.id)(depotlieferungAboMapping.column.depotName -> depot.name)
        }
      }
    }
  }

  def updateTourlieferungenByKunde(kunde: Kunde)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenUpdateRepository.getTourlieferungenByKunde(kunde.id) map { tourlieferung =>
      val bezeichnung = kunde.bezeichnungLieferung getOrElse (kunde.bezeichnung)
      val strasse = kunde.strasseLieferung getOrElse (kunde.strasse)
      val plz = kunde.plzLieferung getOrElse (kunde.plz)
      val ort = kunde.ortLieferung getOrElse (kunde.ort)

      stammdatenUpdateRepository.updateEntity[Tourlieferung, AboId](tourlieferung.id)(
        tourlieferungMapping.column.kundeBezeichnung -> bezeichnung,
        tourlieferungMapping.column.strasse -> strasse,
        tourlieferungMapping.column.hausNummer -> (kunde.hausNummerLieferung orElse kunde.hausNummer),
        tourlieferungMapping.column.adressZusatz -> (kunde.adressZusatzLieferung orElse kunde.adressZusatz),
        tourlieferungMapping.column.plz -> plz,
        tourlieferungMapping.column.ort -> ort
      )
    }
  }

  def handlePersonLoggedIn(personId: PersonId, timestamp: DateTime) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>

      implicit val pid = SystemEvents.SystemPersonId
      stammdatenUpdateRepository.updateEntity[Person, PersonId](personId)(
        personMapping.column.letzteAnmeldung -> Option(timestamp)
      )
    }
  }

  def updateLieferplanungAbotypenListing(lieferplanungId: LieferplanungId)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId) = {
    stammdatenUpdateRepository.getById(lieferplanungMapping, lieferplanungId) map { lp =>
      val lieferungen = selectedZusatzAbo(stammdatenUpdateRepository.getLieferungen(lieferplanungId))
      //delete the zusatzabos that don't even have a korb
      val abotypDates = (lieferungen.map(l => (dateFormat.print(l.datum), l.abotypBeschrieb))
        .groupBy(_._1).mapValues(_ map { _._2 }) map {
          case (datum, abotypBeschriebe) =>
            datum + ": " + abotypBeschriebe.mkString(", ")
        }).mkString("; ")
      if (lp.abotypDepotTour != abotypDates) {
        stammdatenUpdateRepository.updateEntity[Lieferplanung, LieferplanungId](lp.id)(
          lieferplanungMapping.column.abotypDepotTour -> abotypDates
        )
      }
    }
  }

  def selectedZusatzAbo(lieferungen: List[Lieferung])(implicit session: DBSession): List[Lieferung] = {
    lieferungen flatMap { lieferung =>
      stammdatenUpdateRepository.getAbotypById(lieferung.abotypId).get match {
        case z: ZusatzAbotyp => (lieferung.anzahlKoerbeZuLiefern, lieferung.anzahlAbwesenheiten, lieferung.anzahlSaldoZuTief) match {
          case (0, 0, 0) => None
          case (_, _, _) => Some(lieferung)
        }
        case a: Abotyp => Some(lieferung)
      }
    }
  }

  def handleLieferplanungLieferungenChanged(lieferplanungId: LieferplanungId)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      updateLieferplanungAbotypenListing(lieferplanungId)
    }
  }

  def handleLieferplanungModified(entity: Lieferplanung, orig: Lieferplanung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      updateLieferplanungAbotypenListing(entity.id)
    }
  }

  def handleLieferungChanged(entity: Lieferung, orig: Lieferung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      //Berechnung für erste Lieferung durchführen um sicher zu stellen, dass durchschnittspreis auf 0 gesetzt ist
      if (entity.anzahlLieferungen == 1) {
        recalculateLieferungOffen(entity, None)
      }
      val lieferungVorher = stammdatenUpdateRepository.getGeplanteLieferungVorher(orig.vertriebId, entity.datum)
      stammdatenUpdateRepository.getGeplanteLieferungNachher(orig.vertriebId, entity.datum) match {
        case Some(lieferungNach) => recalculateLieferungOffen(lieferungNach, Some(entity))
        case _ =>
      }
    }
  }

  def recalculateLieferungOffen(entity: Lieferung, lieferungVorher: Option[Lieferung])(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    val project = stammdatenUpdateRepository.getProjekt
    val (newDurchschnittspreis, newAnzahlLieferungen) = lieferungVorher match {
      case Some(lieferung) if project.get.geschaftsjahr.isInSame(lieferung.datum.toLocalDate(), entity.datum.toLocalDate()) =>
        val sum = stammdatenUpdateRepository.sumPreisTotalGeplanteLieferungenVorher(entity.vertriebId, entity.datum, project.get.geschaftsjahr.start(entity.datum.toLocalDate()).toDateTimeAtCurrentTime()).getOrElse(BigDecimal(0))

        val durchschnittspreisBisher: BigDecimal = lieferung.anzahlLieferungen match {
          case 0 => BigDecimal(0)
          case _ => sum / lieferung.anzahlLieferungen
        }

        val anzahlLieferungenNeu = lieferung.anzahlLieferungen + 1
        (durchschnittspreisBisher, anzahlLieferungenNeu)
      case _ =>
        (BigDecimal(0), 1)
    }

    val scaled = newDurchschnittspreis.setScale(2, HALF_UP)

    if (entity.durchschnittspreis != scaled || entity.anzahlLieferungen != newAnzahlLieferungen) {
      stammdatenUpdateRepository.updateEntity[Lieferung, LieferungId](entity.id)(
        lieferungMapping.column.durchschnittspreis -> newDurchschnittspreis,
        lieferungMapping.column.anzahlLieferungen -> newAnzahlLieferungen
      )
    }
  }
}
