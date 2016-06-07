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

import ch.openolitor.core.models._
import ch.openolitor.core.domain._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.SqlBinder
import scala.concurrent.ExecutionContext.Implicits.global;
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.buchhaltung.models._
import org.joda.time.DateTime
import scala.util.Random
import scala.concurrent.Future

object StammdatenDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenDBEventEntityListener], sysConfig, system)
}

class DefaultStammdatenDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenDBEventEntityListener(sysConfig) with DefaultStammdatenWriteRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class StammdatenDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with StammdatenDBMappings with AsyncConnectionPoolContextAware {
  this: StammdatenWriteRepositoryComponent =>
  import StammdatenDBEventEntityListener._
  import SystemEvents._

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
    case e @ EntityCreated(personId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboCreated(entity)(personId)
      handleAboCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboDeleted(entity)(personId)
      handleAboDeleted(entity)(personId)
    case e @ EntityCreated(personId, entity: HeimlieferungAbo) => handleHeimlieferungAboCreated(entity)(personId)
    case e @ EntityModified(personId, entity: HeimlieferungAbo, orig: HeimlieferungAbo) => handleHeimlieferungAboModified(entity, orig)(personId)
    case e @ EntityCreated(personId, entity: Abo) => handleAboCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Abo) => handleAboDeleted(entity)(personId)
    case e @ EntityCreated(personId, entity: Abwesenheit) => handleAbwesenheitCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Abwesenheit) => handleAbwesenheitDeleted(entity)(personId)

    case e @ EntityCreated(personId, entity: Kunde) => handleKundeCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Kunde) => handleKundeDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Kunde, orig: Kunde) => handleKundeModified(entity, orig)(personId)

    case e @ EntityCreated(personId, entity: Pendenz) => handlePendenzCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Pendenz) => handlePendenzDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Pendenz, orig: Pendenz) => handlePendenzModified(entity, orig)(personId)

    case e @ EntityCreated(personId, entity: Rechnung) => handleRechnungCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Rechnung) => handleRechnungDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Rechnung, orig: Rechnung) if (orig.status == Erstellt && entity.status == Bezahlt) =>
      handleRechnungBezahlt(entity, orig)(personId)

    case e @ EntityCreated(personId, entity: Lieferplanung) => handleLieferplanungCreated(entity)(personId)

    case e @ EntityModified(personId, entity: Lieferung, orig: Lieferung) => handleLieferungModified(entity, orig)(personId)

    case e @ PersonLoggedIn(personId, timestamp) => handlePersonLoggedIn(personId, timestamp)

    case e @ EntityModified(personId, entity: Vertriebsart, orig: Vertriebsart) => handleVertriebsartModified(entity, orig)(personId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleVertriebsartModified(vertriebsart: Vertriebsart, orig: Vertriebsart)(implicit personId: PersonId) = {
    //update Beschrieb on Vertrieb
  }

  def handleDepotlieferungAboCreated(abo: DepotlieferungAbo)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Depot, DepotId](abo.depotId, { depot =>
        log.debug(s"Add abonnent to depot:${depot.id}")
        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten + 1)
      })
    }
  }

  def handleDepotlieferungAboDeleted(abo: DepotlieferungAbo)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Depot, DepotId](abo.depotId, { depot =>
        log.debug(s"Remove abonnent from depot:${depot.id}")
        depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten - 1)
      })
    }
  }

  def handleAboCreated(abo: Abo)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
        log.debug(s"Add abonnent to abotyp:${abotyp.id}")
        abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten + 1)
      })
      modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
        log.debug(s"Add abonnent to kunde:${kunde.id}")
        kunde.copy(anzahlAbos = kunde.anzahlAbos + 1)
      })
      modifyEntity[Vertrieb, VertriebId](abo.vertriebId, { vertrieb =>
        log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
        vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos + 1)
      })
      modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1)
      })
      modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1)
      })
      modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos + 1)
      })
    }
  }

  def handleAboDeleted(abo: Abo)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
        log.debug(s"Remove abonnent from abotyp:${abotyp.id}")
        abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten - 1)
      })
      modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
        log.debug(s"Remove abonnent from kunde:${kunde.id}")
        kunde.copy(anzahlAbos = kunde.anzahlAbos - 1)
      })
      modifyEntity[Vertrieb, VertriebId](abo.vertriebId, { vertrieb =>
        log.debug(s"Add abonnent to vertrieb:${vertrieb.id}")
        vertrieb.copy(anzahlAbos = vertrieb.anzahlAbos - 1)
      })
      modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1)
      })
      modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1)
      })
      modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
        log.debug(s"Add abonnent to vertriebsart:${vertriebsart.id}")
        vertriebsart.copy(anzahlAbos = vertriebsart.anzahlAbos - 1)
      })
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

    DB localTx { implicit session =>
      stammdatenWriteRepository.getPendenzen(kunde.id) map { pendenz =>
        val copy = pendenz.copy(kundeBezeichnung = kunde.bezeichnung)
        log.debug(s"Modify Kundenbezeichnung on Pendenz to : ${copy.kundeBezeichnung}.")
        stammdatenWriteRepository.updateEntity[Pendenz, PendenzId](copy)
      }
    }

    insertOrUpdateTourlieferungenByKunde(kunde)
  }

  def handleKundeDeleted(kunde: Kunde)(implicit personId: PersonId) = {
    handleKundentypenChanged(kunde.typen, Set())
  }

  def handleKundeCreated(kunde: Kunde)(implicit personId: PersonId) = {
    handleKundentypenChanged(Set(), kunde.typen)
  }

  def handleAbwesenheitDeleted(abw: Abwesenheit)(implicit personId: PersonId) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getProjekt map { projekt =>
        val geschaeftsjahrKey = projekt.geschaftsjahr.key(abw.datum)

        modifyEntity[DepotlieferungAbo, AboId](abw.aboId, { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })
        modifyEntity[HeimlieferungAbo, AboId](abw.aboId, { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })
        modifyEntity[PostlieferungAbo, AboId](abw.aboId, { abo =>
          val value = Math.max(abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ - 1).getOrElse(0), 0)
          log.debug(s"Remove abwesenheit from abo:${abo.id}, new value:$value")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })

        modifyEntity[Lieferung, LieferungId](abw.lieferungId, { lieferung =>
          log.debug(s"Remove abwesenheit from lieferung:${lieferung.id}")
          lieferung.copy(anzahlAbwesenheiten = lieferung.anzahlAbwesenheiten - 1)
        })
      }

      stammdatenWriteRepository.getAboDetail(abw.aboId) match {
        case Some(abo) => {
          stammdatenWriteRepository.getKorb(abw.lieferungId, abw.aboId) match {
            case Some(korb) => {
              stammdatenWriteRepository.getById(abotypMapping, abo.abotypId) map { abotyp =>
                val status = calculateKorbStatus(Some(0), abo.guthaben, abotyp.guthabenMindestbestand)
                val statusAlt = korb.status
                val copy = korb.copy(status = status)
                log.debug(s"Modify Korb-Status as Abwesenheit was deleted : ${copy.status}.")
                stammdatenWriteRepository.updateEntity[Korb, KorbId](copy)
                stammdatenWriteRepository.getById(lieferungMapping, abw.lieferungId) map { lieferung =>
                  updateLieferungCounts(lieferung, status, statusAlt)
                }
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
    DB localTx { implicit session =>
      stammdatenWriteRepository.getProjekt map { projekt =>
        val geschaeftsjahrKey = projekt.geschaftsjahr.key(abw.datum)

        modifyEntity[DepotlieferungAbo, AboId](abw.aboId, { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })
        modifyEntity[HeimlieferungAbo, AboId](abw.aboId, { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })
        modifyEntity[PostlieferungAbo, AboId](abw.aboId, { abo =>
          val value = abo.anzahlAbwesenheiten.get(geschaeftsjahrKey).map(_ + 1).getOrElse(1)
          log.debug(s"Add abwesenheit to abo:${abo.id}, new value:$value, values:${abo.anzahlAbwesenheiten}")
          abo.copy(anzahlAbwesenheiten = abo.anzahlAbwesenheiten.updated(geschaeftsjahrKey, value))
        })

        modifyEntity[Lieferung, LieferungId](abw.lieferungId, { lieferung =>
          log.debug(s"Add abwesenheit to lieferung:${lieferung.id}")
          lieferung.copy(anzahlAbwesenheiten = lieferung.anzahlAbwesenheiten + 1)
        })
      }

      stammdatenWriteRepository.getKorb(abw.lieferungId, abw.aboId) match {
        case Some(korb) => {
          val statusAlt = korb.status
          val copy = korb.copy(status = FaelltAusAbwesend)
          log.debug(s"Modify Korb-Status as Abwesenheit was created : ${copy.status}.")
          stammdatenWriteRepository.updateEntity[Korb, KorbId](copy)
          stammdatenWriteRepository.getById(lieferungMapping, abw.lieferungId) map { lieferung =>
            updateLieferungCounts(lieferung, FaelltAusAbwesend, statusAlt)
          }
        }
        case None => log.debug(s"No Korb yet for Lieferung : ${abw.lieferungId} and Abotyp : ${abw.aboId}")
      }
    }
  }

  def updateLieferungCounts(lieferung: Lieferung, korbStatusNeu: KorbStatus, korbStatusAlt: KorbStatus)(implicit personId: PersonId) = {
    val zuLiefenDec = if (korbStatusAlt == WirdGeliefert && korbStatusNeu != WirdGeliefert) 1 else 0
    val abwDec = if (korbStatusAlt == FaelltAusAbwesend && korbStatusNeu != FaelltAusAbwesend) 1 else 0
    val saldoDec = if (korbStatusAlt == FaelltAusSaldoZuTief && korbStatusNeu != FaelltAusSaldoZuTief) 1 else 0

    val zuLiefenAdd = if (korbStatusNeu == WirdGeliefert && korbStatusAlt != WirdGeliefert) 1 else 0
    val abwAdd = if (korbStatusNeu == FaelltAusAbwesend && korbStatusNeu != FaelltAusAbwesend) 1 else 0
    val saldoAdd = if (korbStatusNeu == FaelltAusSaldoZuTief && korbStatusAlt != FaelltAusSaldoZuTief) 1 else 0
    val lCopy = lieferung.copy(
      anzahlKoerbeZuLiefern = lieferung.anzahlKoerbeZuLiefern - zuLiefenDec + zuLiefenAdd,
      anzahlAbwesenheiten = lieferung.anzahlAbwesenheiten - abwDec + abwAdd,
      anzahlSaldoZuTief = lieferung.anzahlSaldoZuTief - saldoDec + saldoAdd
    )
    log.debug(s"Modify Lieferung as Korb-Status was modified : ${lieferung.id} status form ${korbStatusNeu} to ${korbStatusAlt}.")
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lCopy)
    }
  }

  def handlePendenzCreated(pendenz: Pendenz)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Kunde, KundeId](pendenz.kundeId, { kunde =>
        log.debug(s"Add pendenz count to kunde:${kunde.id}")
        kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen + 1)
      })
    }
  }

  def handlePendenzDeleted(pendenz: Pendenz)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[Kunde, KundeId](pendenz.kundeId, { kunde =>
        log.debug(s"Remove pendenz count from kunde:${kunde.id}")
        kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen - 1)
      })
    }
  }

  def handlePendenzModified(pendenz: Pendenz, orig: Pendenz)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      if (pendenz.status == Erledigt && orig.status != Erledigt) {
        modifyEntity[Kunde, KundeId](pendenz.kundeId, { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen - 1)
        })
      } else if (pendenz.status != Erledigt && orig.status == Erledigt) {
        modifyEntity[Kunde, KundeId](pendenz.kundeId, { kunde =>
          log.debug(s"Remove pendenz count from kunde:${kunde.id}")
          kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen + 1)
        })
      }
    }
  }

  def handleKundentypenChanged(removed: Set[KundentypId], added: Set[KundentypId])(implicit personId: PersonId) = {
    DB localTx { implicit session =>
      val kundetypen = stammdatenWriteRepository.getKundentypen
      removed.map { kundetypId =>
        kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
          case customKundentyp: CustomKundentyp =>
            val copy = customKundentyp.copy(anzahlVerknuepfungen = customKundentyp.anzahlVerknuepfungen - 1)
            log.debug(s"Reduce anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${copy.anzahlVerknuepfungen}")
            stammdatenWriteRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
        }
      }

      added.map { kundetypId =>
        kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
          case customKundentyp: CustomKundentyp =>
            val copy = customKundentyp.copy(anzahlVerknuepfungen = customKundentyp.anzahlVerknuepfungen + 1)
            log.debug(s"Increment anzahlVerknuepfung on CustomKundentyp: ${customKundentyp.kundentyp}. New count:${copy.anzahlVerknuepfungen}")
            stammdatenWriteRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
        }
      }
    }

  }

  def handleRechnungDeleted(rechnung: Rechnung)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[DepotlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen
        )
      })
      modifyEntity[PostlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen
        )
      })
      modifyEntity[HeimlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen
        )
      })
    }
  }

  def handleRechnungCreated(rechnung: Rechnung)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[DepotlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung + rechnung.anzahlLieferungen
        )
      })
      modifyEntity[PostlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung + rechnung.anzahlLieferungen
        )
      })
      modifyEntity[HeimlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung + rechnung.anzahlLieferungen
        )
      })
    }
  }

  def handleRechnungBezahlt(rechnung: Rechnung, orig: Rechnung)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      modifyEntity[DepotlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      })
      modifyEntity[PostlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      })
      modifyEntity[HeimlieferungAbo, AboId](rechnung.aboId, { abo =>
        abo.copy(
          guthabenInRechnung = abo.guthabenInRechnung - rechnung.anzahlLieferungen,
          guthaben = abo.guthaben + rechnung.anzahlLieferungen,
          guthabenVertraglich = abo.guthabenVertraglich map (_ - rechnung.anzahlLieferungen) orElse (None)
        )
      })
    }
  }

  def handleLieferplanungCreated(lieferplanung: Lieferplanung)(implicit personId: PersonId) = {

  }

  def handleLieferungModified(lieferung: Lieferung, orig: Lieferung)(implicit personId: PersonId) = {
    logger.debug(s"handleLieferungModified: lieferung:\n$lieferung\norig:$orig")
    if (lieferung.lieferplanungId.isDefined && !orig.lieferplanungId.isDefined) {
      //Lieferung was planed in a Lieferplanung
      createKoerbe(lieferung.id)
    }
    if (!lieferung.lieferplanungId.isDefined && orig.lieferplanungId.isDefined) {
      //Lieferung was removed in a Lieferplanung
      removeKoerbe(lieferung.id)
    }
  }

  def removeKoerbe(lieferungId: LieferungId)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteKoerbe(lieferungId)
    }
  }

  def createKoerbe(lieferungId: LieferungId)(implicit personId: PersonId) = {
    logger.debug(s"Create Koerbe:$lieferungId")
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(lieferungMapping, lieferungId) map { lieferung =>
        stammdatenWriteRepository.getById(abotypMapping, lieferung.abotypId) map { abotyp =>
          val abos = stammdatenWriteRepository.getAktiveAbos(lieferung.vertriebId, lieferung.datum)
          val statusL = abos map { abo =>
            val abwCount = stammdatenWriteRepository.countAbwesend(lieferungId, abo.id)
            val retAbw = abwCount match {
              case Some(abw) if abw > 0 => 1
              case _ => 0
            }
            val status = calculateKorbStatus(abwCount, abo.guthaben, abotyp.guthabenMindestbestand)
            val kId = KorbId(Random.nextLong)
            val korb = Korb(
              kId,
              lieferungId,
              abo.id,
              status,
              abo.guthaben,
              DateTime.now,
              personId,
              DateTime.now,
              personId
            )
            stammdatenWriteRepository.insertEntity[Korb, KorbId](korb)
            status
          }

          val counts = statusL.groupBy { _.getClass }.mapValues(_.size)

          val copy = lieferung.copy(
            anzahlKoerbeZuLiefern = counts.get(WirdGeliefert.getClass).getOrElse(0),
            anzahlAbwesenheiten = counts.get(FaelltAusAbwesend.getClass).getOrElse(0),
            anzahlSaldoZuTief = counts.get(FaelltAusSaldoZuTief.getClass).getOrElse(0)
          )
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](copy)

        }
      }
    }
  }

  def handleHeimlieferungAboModified(entity: HeimlieferungAbo, orig: HeimlieferungAbo)(implicit personId: PersonId) = {
    insertOrUpdateTourlieferung(entity)
  }

  def handleHeimlieferungAboCreated(entity: HeimlieferungAbo)(implicit personId: PersonId) = {
    handleAboCreated(entity)(personId)
    insertOrUpdateTourlieferung(entity)
  }

  def insertOrUpdateTourlieferung(entity: HeimlieferungAbo)(implicit personId: PersonId) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(kundeMapping, entity.kundeId) map { kunde =>
        val updated = Tourlieferung(
          entity.id,
          entity.tourId,
          entity.abotypId,
          entity.kundeId,
          entity.vertriebsartId,
          entity.vertriebId,
          kunde.bezeichnungLieferung getOrElse kunde.bezeichnung,
          kunde.strasseLieferung getOrElse kunde.strasse,
          kunde.hausNummerLieferung orElse kunde.hausNummer,
          kunde.adressZusatzLieferung orElse kunde.adressZusatz,
          kunde.plzLieferung getOrElse kunde.plz,
          kunde.ortLieferung getOrElse kunde.ort,
          entity.abotypName,
          None,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )

        stammdatenWriteRepository.getById(tourlieferungMapping, entity.id) map { tourlieferung =>
          stammdatenWriteRepository.updateEntity[Tourlieferung, AboId](updated.copy(sort = tourlieferung.sort))
        } getOrElse {
          stammdatenWriteRepository.insertEntity[Tourlieferung, AboId](updated)
        }
      }
    }
  }

  def insertOrUpdateTourlieferungenByKunde(kunde: Kunde)(implicit personId: PersonId) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getTourlieferungenByKunde(kunde.id) map { tourlieferung =>
        stammdatenWriteRepository.updateEntity[Tourlieferung, AboId](tourlieferung.copy(
          kundeBezeichnung = kunde.bezeichnungLieferung getOrElse kunde.bezeichnung,
          strasse = kunde.strasseLieferung getOrElse kunde.strasse,
          hausNummer = kunde.hausNummerLieferung orElse kunde.hausNummer,
          adressZusatz = kunde.adressZusatzLieferung orElse kunde.adressZusatz,
          plz = kunde.plzLieferung getOrElse kunde.plz,
          ort = kunde.ortLieferung getOrElse kunde.ort
        ))
      }
    }
  }

  def handlePersonLoggedIn(personId: PersonId, timestamp: DateTime) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(personMapping, personId) map { person =>
        implicit val pid = SystemEvents.SystemPersonId
        val updated = person.copy(letzteAnmeldung = Some(timestamp))
        stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
      }
    }
  }

  def calculateKorbStatus(abwCount: Option[Int], guthaben: Int, minGuthaben: Int): KorbStatus = {
    (abwCount, guthaben) match {
      case (Some(abw), gut) if abw > 0 => FaelltAusAbwesend
      case (_, gut) if gut > minGuthaben => WirdGeliefert
      case (_, gut) if gut <= minGuthaben => FaelltAusSaldoZuTief
    }
  }

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E
  )(implicit session: DBSession, syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], personId: PersonId) = {

    stammdatenWriteRepository.getById(syntax, id) map { result =>
      val copy = mod(result)
      stammdatenWriteRepository.updateEntity[E, I](copy)
    }
  }
}
