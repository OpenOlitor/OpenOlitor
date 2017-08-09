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
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.buchhaltung.models._
import ch.openolitor.util.IdUtil
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
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
  import StammdatenDBEventEntityListener._
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
      handleHeimlieferungAboModified(orig, entity)(personId)
      handleAboModified(orig, entity)(personId)
    case e @ EntityModified(personId, entity: HeimlieferungAbo, orig: HeimlieferungAbo) =>
      handleHeimlieferungAboModified(entity, orig)(personId)
      handleAboModified(orig, entity)(personId)
    case e @ EntityModified(personId, entity: PostlieferungAbo, orig: PostlieferungAbo) if entity.vertriebId != orig.vertriebId =>
      handleAboModified(orig, entity)(personId)
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

    case e @ EntityCreated(personId, entity: Rechnung) => handleRechnungCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Rechnung) => handleRechnungDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Rechnung, orig: Rechnung) if (orig.status != Bezahlt && entity.status == Bezahlt) =>
      handleRechnungBezahlt(entity, orig)(personId)
    case e @ EntityModified(personId, entity: Rechnung, orig: Rechnung) if entity.anzahlLieferungen != orig.anzahlLieferungen =>
      handleRechnungGuthabenModified(entity, orig)(personId)

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

    case e @ EntityCreated(personId, entity: Korb) => handleKorbCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Korb) => handleKorbDeleted(entity)(personId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleAbotypModify(orig: Abotyp, entity: Abotyp)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getAbosByAbotyp(entity.id) map { abo =>
        modifyEntity[DepotlieferungAbo, AboId](abo.id) { abo =>
          abo.copy(abotypName = entity.name)
        }
        modifyEntity[HeimlieferungAbo, AboId](abo.id) { abo =>
          abo.copy(abotypName = entity.name)
        }
        modifyEntity[PostlieferungAbo, AboId](abo.id) { abo =>
          abo.copy(abotypName = entity.name)
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
      modifyEntity[Depot, DepotId](abo.depotId) { depot =>
        log.debug(s"Add abonnent to depot:${depot.id}")

        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten + 1, anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
      }
    }
  }

  def handleDepotlieferungAboDeleted(abo: DepotlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Depot, DepotId](abo.depotId) { depot =>
        log.debug(s"Remove abonnent from depot:${depot.id}")
        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten - 1, anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
      }
    }
  }

  def handleDepotlieferungAboDepotChanged(abo: DepotlieferungAbo, from: DepotId, to: DepotId)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Depot, DepotId](from) { depot =>
        log.debug(s"Remove abonnent from depot:${depot.id}")
        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten - 1, anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
      }
      modifyEntity[Depot, DepotId](to) { depot =>
        log.debug(s"Add abonnent to depot:${depot.id}")
        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten + 1, anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
      }
    }
  }

  def handleHeimlieferungAboModified(entity: HeimlieferungAbo, orig: HeimlieferungAbo)(implicit personId: PersonId) = {
    updateTourlieferung(entity)
  }

  def handleHeimlieferungAboCreated(entity: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Tour, TourId](entity.tourId) { tour =>
        log.debug(s"Add abonnent to tour:${tour.id}")
        tour.copy(anzahlAbonnenten = tour.anzahlAbonnenten + 1, anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv + calculateAboAktivCreate(entity))
      }
    }
    updateTourlieferung(entity)
  }

  def handleHeimlieferungAboDeleted(abo: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Tour, TourId](abo.tourId) { tour =>
        log.debug(s"Remove abonnent from tour:${tour.id}")
        tour.copy(anzahlAbonnenten = tour.anzahlAbonnenten - 1, anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
      }
    }
  }

  def handleHeimlieferungAboTourChanged(abo: HeimlieferungAbo, from: TourId, to: TourId)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Tour, TourId](from) { tour =>
        log.debug(s"Remove abonnent from tour:${tour.id}")
        tour.copy(anzahlAbonnenten = tour.anzahlAbonnenten - 1, anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv - calculateAboAktivCreate(abo))
      }
      modifyEntity[Tour, TourId](to) { tour =>
        log.debug(s"Add abonnent to tour:${tour.id}")
        tour.copy(anzahlAbonnenten = tour.anzahlAbonnenten + 1, anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv + calculateAboAktivCreate(abo))
      }
    }
  }

  def handleAboCreated(abo: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modAboCount = calculateAboAktivCreate(abo)
      modifyEntity[Abotyp, AbotypId](abo.abotypId) { abotyp =>
        log.debug(s"Add abonnent to abotyp:${abotyp.id}")
        abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten + 1, anzahlAbonnentenAktiv = abotyp.anzahlAbonnentenAktiv + modAboCount)
      }
      modifyEntity[Kunde, KundeId](abo.kundeId) { kunde =>
        log.debug(s"Add abonnent to kunde:${kunde.id}")
        kunde.copy(anzahlAbos = kunde.anzahlAbos + 1, anzahlAbosAktiv = kunde.anzahlAbosAktiv + modAboCount)
      }
      modifyEntity[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
        log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
        vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos + 1, anzahlAbosAktiv = vertrieb.anzahlAbosAktiv + modAboCount)
      }
      modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + modAboCount)
      }
      modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + modAboCount)
      }
      modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + modAboCount)
      }
    }
  }

  def handleAboModified(from: Abo, to: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (from.aktiv != to.aktiv) {
        handleAboAktivChange(to, if (to.aktiv) 1 else -1)
      }

      if (from.vertriebId != to.vertriebId) {
        val modAboCount = calculateAboAktivCreate(to)
        modifyEntity[Vertrieb, VertriebId](from.vertriebId) { vertrieb =>
          log.debug(s"Remove abonnent from vertrieb:${vertrieb.id}")
          vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos - 1, anzahlAbosAktiv = vertrieb.anzahlAbosAktiv - modAboCount)
        }
        modifyEntity[Vertrieb, VertriebId](to.vertriebId) { vertrieb =>
          log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
          vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos + 1, anzahlAbosAktiv = vertrieb.anzahlAbosAktiv + modAboCount)
        }
      }
    }
  }

  def handleAboDeleted(abo: Abo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val modAboCount = calculateAboAktivCreate(abo)
      modifyEntity[Abotyp, AbotypId](abo.abotypId) { abotyp =>
        log.debug(s"Remove abonnent from abotyp:${abotyp.id}")
        abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten - 1, anzahlAbonnentenAktiv = abotyp.anzahlAbonnentenAktiv - modAboCount)
      }
      modifyEntity[Kunde, KundeId](abo.kundeId) { kunde =>
        log.debug(s"Remove abonnent from kunde:${kunde.id}")
        kunde.copy(anzahlAbos = kunde.anzahlAbos - 1, anzahlAbosAktiv = kunde.anzahlAbosAktiv - modAboCount)
      }
      modifyEntity[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
        log.debug(s"Remove abonnent from vertrieb:${vertrieb.id}")
        vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos - 1, anzahlAbosAktiv = vertrieb.anzahlAbosAktiv - modAboCount)
      }
      modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv - modAboCount)
      }
      modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv - modAboCount)
      }
      modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
        log.debug(s"Remove abonnent from vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1, anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv - modAboCount)
      }
    }
  }

  def handleKorbDeleted(korb: Korb)(implicit personId: PersonId) = {
    updateLieferungWithCount(korb, -1)
  }

  def handleKorbCreated(korb: Korb)(implicit personId: PersonId) = {
    updateLieferungWithCount(korb, +1)
  }

  private def updateLieferungWithCount(korb: Korb, add: Int)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getById(lieferungMapping, korb.lieferungId) map { lieferung =>
        val copy = lieferung.copy(
          anzahlKoerbeZuLiefern = if (WirdGeliefert == korb.status) lieferung.anzahlKoerbeZuLiefern + add else lieferung.anzahlKoerbeZuLiefern,
          anzahlAbwesenheiten = if (FaelltAusAbwesend == korb.status) lieferung.anzahlAbwesenheiten + add else lieferung.anzahlAbwesenheiten,
          anzahlSaldoZuTief = if (FaelltAusSaldoZuTief == korb.status) lieferung.anzahlSaldoZuTief + add else lieferung.anzahlSaldoZuTief
        )

        stammdatenUpdateRepository.updateEntity[Lieferung, LieferungId](copy, lieferungMapping.column.anzahlKoerbeZuLiefern, lieferungMapping.column.anzahlAbwesenheiten, lieferungMapping.column.anzahlSaldoZuTief)
      }
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
        val copy = pendenz.copy(kundeBezeichnung = kunde.bezeichnung)
        log.debug(s"Modify Kundenbezeichnung on Pendenz to : ${copy.kundeBezeichnung}.")
        stammdatenUpdateRepository.updateEntity[Pendenz, PendenzId](copy)
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
        stammdatenUpdateRepository.getById(kundeMapping, person.kundeId) map { kunde =>
          val copy = kunde.copy(bezeichnung = personen.head.fullName)
          log.debug(s"Kunde-Bezeichnung set to empty as there is just one Person: ${kunde.id}")
          stammdatenUpdateRepository.updateEntity[Kunde, KundeId](copy)
        }
      }
    }
  }

  def handleAbwesenheitDeleted(abw: Abwesenheit)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getProjekt map { projekt =>
        val geschaeftsjahrKey = projekt.geschaftsjahr.key(abw.datum)

        modifyEntity[DepotlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
        modifyEntity[HeimlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
        modifyEntity[PostlieferungAbo, AboId](abw.aboId) { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
      }

      stammdatenUpdateRepository.getAboDetailAusstehend(abw.aboId) match {
        case Some(abo) => {
          stammdatenUpdateRepository.getKorb(abw.lieferungId, abw.aboId) match {
            case Some(korb) => {
              stammdatenUpdateRepository.getById(abotypMapping, abo.abotypId) map { abotyp =>
                //re count because the might be another abwesenheit for the same date
                val newAbwesenheitCount = stammdatenUpdateRepository.countAbwesend(abw.aboId, abw.datum)
                val status = calculateKorbStatus(newAbwesenheitCount, abo.guthaben, abotyp.guthabenMindestbestand)
                val statusAlt = korb.status
                val copy = korb.copy(status = status)
                log.debug(s"Modify Korb-Status as Abwesenheit was deleted ${abw.id}, newCount:$newAbwesenheitCount, newStatus:${copy.status}.")
                stammdatenUpdateRepository.updateEntity[Korb, KorbId](copy)
              }
            }
            case None => log.debug(s"No Korb yet for Lieferung : ${abw.lieferungId} and Abotyp : ${abw.aboId}")
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

        modifyEntity[DepotlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
        modifyEntity[HeimlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
        modifyEntity[PostlieferungAbo, AboId](abw.aboId) { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        }
      }

      stammdatenUpdateRepository.getKorb(abw.lieferungId, abw.aboId) match {
        case Some(korb) => {
          val statusAlt = korb.status
          val status = FaelltAusAbwesend
          val copy = korb.copy(status = status)
          log.debug(s"Modify Korb-Status as Abwesenheit was created : ${copy}.")
          stammdatenUpdateRepository.updateEntity[Korb, KorbId](copy)

        }
        case None => log.debug(s"No Korb yet for Lieferung : ${abw.lieferungId} and Abotyp : ${abw.aboId}")
      }
    }
  }

  def handleKorbStatusChanged(korb: Korb, statusAlt: KorbStatus)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getById(lieferungMapping, korb.lieferungId) map { lieferung =>
        log.debug(s"Korb Status changed:${korb.aboId}/${korb.lieferungId}")
        stammdatenUpdateRepository.updateEntity[Lieferung, LieferungId](recalculateLieferungCounts(lieferung, korb.status, statusAlt), lieferungMapping.column.anzahlKoerbeZuLiefern, lieferungMapping.column.anzahlAbwesenheiten, lieferungMapping.column.anzahlSaldoZuTief)
      }
    }
  }

  private def recalculateLieferungCounts(lieferung: Lieferung, korbStatusNeu: KorbStatus, korbStatusAlt: KorbStatus)(implicit personId: PersonId, session: DBSession) = {
    val zuLiefernDiff = korbStatusNeu match {
      case WirdGeliefert => 1
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
      modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
        log.debug(s"Add pendenz count to kunde:${kunde.id}")
        kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen + 1)
      }
    }
  }

  def handlePendenzDeleted(pendenz: Pendenz)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
        log.debug(s"Remove pendenz count from kunde:${kunde.id}")
        kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen - 1)
      }
    }
  }

  def handlePendenzModified(pendenz: Pendenz, orig: Pendenz)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (pendenz.status == Erledigt && orig.status != Erledigt) {
        modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen - 1)
        }
      } else if (pendenz.status != Erledigt && orig.status == Erledigt) {
        modifyEntity[Kunde, KundeId](pendenz.kundeId) { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen + 1)
        }
      }
    }
  }

  def handleKundentypenChanged(removed: Set[KundentypId], added: Set[KundentypId])(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val kundetypen = stammdatenUpdateRepository.getKundentypen
      removed.map { kundetypId =>
        kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
          case customKundentyp: CustomKundentyp =>
            val copy = customKundentyp.copy(anzahlVerknuepfungen = customKundentyp.anzahlVerknuepfungen - 1)
            log.debug(s"Reduce anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${copy.anzahlVerknuepfungen}")
            stammdatenUpdateRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
        }
      }

      added.map { kundetypId =>
        kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
          case customKundentyp: CustomKundentyp =>
            val copy = customKundentyp.copy(anzahlVerknuepfungen = customKundentyp.anzahlVerknuepfungen + 1)
            log.debug(s"Increment anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${copy.anzahlVerknuepfungen}")
            stammdatenUpdateRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
        }
      }
    }

  }

  def handleRechnungDeleted(rechnung: Rechnung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      adjustGuthabenInRechnung(rechnung.aboId, 0 - rechnung.anzahlLieferungen)
    }
  }

  def handleRechnungCreated(rechnung: Rechnung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      adjustGuthabenInRechnung(rechnung.aboId, rechnung.anzahlLieferungen)
    }
  }

  def handleRechnungBezahlt(rechnung: Rechnung, orig: Rechnung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      modifyEntity[DepotlieferungAbo, AboId](rechnung.aboId) { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      }
      modifyEntity[PostlieferungAbo, AboId](rechnung.aboId) { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      }
      modifyEntity[HeimlieferungAbo, AboId](rechnung.aboId) { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      }
    }
  }

  def handleRechnungGuthabenModified(rechnung: Rechnung, orig: Rechnung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      adjustGuthabenInRechnung(rechnung.aboId, rechnung.anzahlLieferungen - orig.anzahlLieferungen)
    }
  }

  private def adjustGuthabenInRechnung(aboId: AboId, diff: Int)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    modifyEntity[DepotlieferungAbo, AboId](aboId) { abo =>
      abo.copy(
        guthabenInRechnung = abo.guthabenInRechnung + diff
      )
    }
    modifyEntity[PostlieferungAbo, AboId](aboId) { abo =>
      abo.copy(
        guthabenInRechnung = abo.guthabenInRechnung + diff
      )
    }
    modifyEntity[HeimlieferungAbo, AboId](aboId) { abo =>
      abo.copy(
        guthabenInRechnung = abo.guthabenInRechnung + diff
      )
    }
  }

  def handleLieferplanungCreated(lieferplanung: Lieferplanung)(implicit personId: PersonId) = {
  }

  def handleLieferplanungDeleted(lieferplanung: Lieferplanung)(implicit personId: PersonId) = {

  }

  def handleAuslieferungAusgeliefert(entity: Auslieferung)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getKoerbe(entity.id) map { korb =>
        val copy = korb.copy(status = Geliefert)
        stammdatenUpdateRepository.updateEntity[Korb, KorbId](copy)
        stammdatenUpdateRepository.getProjekt map { projekt =>
          val geschaeftsjahrKey = projekt.geschaftsjahr.key(entity.datum.toLocalDate)

          modifyEntity[DepotlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            abo.copy(guthaben = korb.guthabenVorLieferung - 1, letzteLieferung = getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              anzahlLieferungen = abo.anzahlLieferungen.updated(geschaeftsjahrKey, value))
          }
          modifyEntity[HeimlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            abo.copy(guthaben = korb.guthabenVorLieferung - 1, letzteLieferung = getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              anzahlLieferungen = abo.anzahlLieferungen.updated(geschaeftsjahrKey, value))
          }
          modifyEntity[PostlieferungAbo, AboId](korb.aboId) { abo =>
            val value = abo.anzahlLieferungen.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
            updateAbotypOnAusgeliefert(abo.abotypId, entity.datum)
            abo.copy(guthaben = korb.guthabenVorLieferung - 1, letzteLieferung = getLatestDate(abo.letzteLieferung, Some(entity.datum)),
              anzahlLieferungen = abo.anzahlLieferungen.updated(geschaeftsjahrKey, value))
          }
        }
      }
    }
  }

  private def updateAbotypOnAusgeliefert(abotypId: AbotypId, letzteLieferung: DateTime)(implicit personId: PersonId, dbSession: DBSession, publisher: EventPublisher) = {
    modifyEntity[Abotyp, AbotypId](abotypId) { abotyp =>
      abotyp.copy(letzteLieferung = getLatestDate(abotyp.letzteLieferung, Some(letzteLieferung)))
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
          val copy = abo.copy(
            depotName = depot.name
          )
          stammdatenUpdateRepository.updateEntity[DepotlieferungAbo, AboId](copy)
        }
      }
    }
  }

  def updateTourlieferung(entity: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getById(kundeMapping, entity.kundeId) map { kunde =>
        stammdatenUpdateRepository.getById(tourlieferungMapping, entity.id) map { tourlieferung =>
          stammdatenUpdateRepository.updateEntity[Tourlieferung, AboId](Tourlieferung(entity, kunde, personId).copy(sort = tourlieferung.sort))
        }
      }
    }
  }

  def updateTourlieferungenByKunde(kunde: Kunde)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher) = {
    stammdatenUpdateRepository.getTourlieferungenByKunde(kunde.id) map { tourlieferung =>
      stammdatenUpdateRepository.updateEntity[Tourlieferung, AboId](tourlieferung.copy(
        kundeBezeichnung = kunde.bezeichnungLieferung getOrElse kunde.bezeichnung,
        strasse = kunde.strasseLieferung getOrElse kunde.strasse,
        hausNummer = kunde.hausNummerLieferung orElse kunde.hausNummer,
        adressZusatz = kunde.adressZusatzLieferung orElse kunde.adressZusatz,
        plz = kunde.plzLieferung getOrElse kunde.plz,
        ort = kunde.ortLieferung getOrElse kunde.ort
      ))
    }
  }

  def handlePersonLoggedIn(personId: PersonId, timestamp: DateTime) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenUpdateRepository.getById(personMapping, personId) map { person =>
        implicit val pid = SystemEvents.SystemPersonId
        val updated = person.copy(letzteAnmeldung = Some(timestamp))
        stammdatenUpdateRepository.updateEntity[Person, PersonId](updated)
      }
    }
  }

  def updateLieferplanungAbotypenListing(lieferplanungId: LieferplanungId)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId) = {
    stammdatenUpdateRepository.getById(lieferplanungMapping, lieferplanungId) map { lp =>
      val lieferungen = stammdatenUpdateRepository.getLieferungen(lieferplanungId)
      val abotypDates = (lieferungen.map(l => (dateFormat.print(l.datum), l.abotypBeschrieb))
        .groupBy(_._1).mapValues(_ map { _._2 }) map {
          case (datum, abotypBeschriebe) =>
            datum + ": " + abotypBeschriebe.mkString(", ")
        }).mkString("; ")
      val copy = lp.copy(abotypDepotTour = abotypDates)
      if (lp.abotypDepotTour != abotypDates) {
        stammdatenUpdateRepository.updateEntity[Lieferplanung, LieferplanungId](copy)
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
      val updatedLieferung = entity.copy(
        durchschnittspreis = newDurchschnittspreis,
        anzahlLieferungen = newAnzahlLieferungen,
        modifidat = DateTime.now,
        modifikator = personId
      )
      stammdatenUpdateRepository.updateEntity[Lieferung, LieferungId](
        updatedLieferung,
        lieferungMapping.column.durchschnittspreis,
        lieferungMapping.column.anzahlLieferungen,
        lieferungMapping.column.modifidat,
        lieferungMapping.column.modifikator
      )
    }
  }

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](id: I)(mod: E => E)(implicit session: DBSession, publisher: EventPublisher, syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], personId: PersonId): Option[E] = {
    modifyEntityWithRepository(stammdatenUpdateRepository)(id, mod)
  }
}
