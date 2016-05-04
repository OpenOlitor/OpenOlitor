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
import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.buchhaltung.models.{ Erstellt, Bezahlt }

object StammdatenDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenDBEventEntityListener], sysConfig, system)
}

class DefaultStammdatenDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenDBEventEntityListener(sysConfig) with DefaultStammdatenWriteRepositoryComponent with DefaultStammdatenReadRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class StammdatenDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with StammdatenDBMappings with AsyncConnectionPoolContextAware {
  this: StammdatenWriteRepositoryComponent with StammdatenReadRepositoryComponent =>
  import StammdatenDBEventEntityListener._

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityCreated(userId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboCreated(entity)(userId)
      handleAboCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboDeleted(entity)(userId)
      handleAboDeleted(entity)(userId)
    case e @ EntityCreated(userId, entity: Abo) => handleAboCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Abo) => handleAboDeleted(entity)(userId)
    case e @ EntityCreated(userId, entity: Abwesenheit) => handleAbwesenheitCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Abwesenheit) => handleAbwesenheitDeleted(entity)(userId)

    case e @ EntityCreated(userId, entity: Kunde) => handleKundeCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Kunde) => handleKundeDeleted(entity)(userId)
    case e @ EntityModified(userId, entity: Kunde, orig: Kunde) => handleKundeModified(entity, orig)(userId)

    case e @ EntityCreated(userId, entity: Pendenz) => handlePendenzCreated(entity)(userId)

    case e @ EntityCreated(userId, entity: Rechnung) => handleRechnungCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Rechnung) => handleRechnungDeleted(entity)(userId)
    case e @ EntityModified(userId, entity: Rechnung, orig: Rechnung) if (orig.status == Erstellt && entity.status == Bezahlt) =>
      handleRechnungBezahlt(entity, orig)(userId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleDepotlieferungAboCreated(abo: DepotlieferungAbo)(implicit userId: UserId) = {
    modifyEntity[Depot, DepotId](abo.depotId, { depot =>
      log.debug(s"Add abonnent to depot:${depot.id}")
      depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten + 1)
    })
  }

  def handleDepotlieferungAboDeleted(abo: DepotlieferungAbo)(implicit userId: UserId) = {
    modifyEntity[Depot, DepotId](abo.depotId, { depot =>
      log.debug(s"Remove abonnent from depot:${depot.id}")
      depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten - 1)
    })
  }

  def handleAboCreated(abo: Abo)(implicit userId: UserId) = {
    modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
      log.debug(s"Add abonnent to abotyp:${abotyp.id}")
      abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten + 1)
    })
    modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
      log.debug(s"Add abonnent to kunde:${kunde.id}")
      kunde.copy(anzahlAbos = kunde.anzahlAbos + 1)
    })
  }

  def handleAboDeleted(abo: Abo)(implicit userId: UserId) = {
    modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
      log.debug(s"Remove abonnent from abotyp:${abotyp.id}")
      abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten - 1)
    })
    modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
      log.debug(s"Remove abonnent from kunde:${kunde.id}")
      kunde.copy(anzahlAbos = kunde.anzahlAbos - 1)
    })
  }

  def handleKundeModified(kunde: Kunde, orig: Kunde)(implicit userId: UserId) = {
    //compare typen
    //find removed typen
    val removed = orig.typen -- kunde.typen

    //tag typen which where added
    val added = kunde.typen -- orig.typen

    log.debug(s"Kunde ${kunde.bezeichnung} modified, handle CustomKundentypen. Orig: ${orig.typen} -> modified: ${kunde.typen}. Removed typen:${removed}, added typen:${added}")

    handleKundentypenChanged(removed, added)

    //TODO Update kundeBezeichnung on attached Pendenzen
    //    modifyEntity[Pendenz, PendenzId](kunde.id, { pendenz =>
    //      log.debug(s"Update kundeBezeichnung on all Pendenzen:${pendenz.id}")
    //      pendenz.copy(kundeBezeichnung = kunde.bezeichnung)
    //    })
  }

  def handleKundeDeleted(kunde: Kunde)(implicit userId: UserId) = {
    handleKundentypenChanged(kunde.typen, Set())
  }

  def handleKundeCreated(kunde: Kunde)(implicit userId: UserId) = {
    handleKundentypenChanged(Set(), kunde.typen)
  }

  def handleAbwesenheitDeleted(abw: Abwesenheit)(implicit userId: UserId) = {
    //TODO: calculate geschaeftsjahr key based on project configuration
    val geschaeftsjahrKey = abw.datum.year.getAsText

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

  def handleAbwesenheitCreated(abw: Abwesenheit)(implicit userId: UserId) = {
    //TODO: calculate geschaeftsjahr key based on project configuration
    val geschaeftsjahrKey = abw.datum.year().getAsText

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

  def handlePendenzCreated(pendenz: Pendenz)(implicit userId: UserId) = {
    modifyEntity[Kunde, KundeId](pendenz.kundeId, { kunde =>
      log.debug(s"Add pendenz count to kunde:${kunde.id}")
      kunde.copy(anzahlPendenzen = kunde.anzahlPendenzen + 1)
    })
  }

  def handleKundentypenChanged(removed: Set[KundentypId], added: Set[KundentypId])(implicit userId: UserId) = {
    stammdatenReadRepository.getKundentypen map { kundetypen =>
      DB autoCommit { implicit session =>
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

  }

  def handleRechnungDeleted(rechnung: Rechnung)(implicit userId: UserId) = {
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

  def handleRechnungCreated(rechnung: Rechnung)(implicit userId: UserId) = {
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

  def handleRechnungBezahlt(rechnung: Rechnung, orig: Rechnung)(implicit userId: UserId) = {
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

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E
  )(implicit syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], userId: UserId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(syntax, id) map { result =>
        val copy = mod(result)
        stammdatenWriteRepository.updateEntity[E, I](copy)
      }
    }
  }
}
