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

import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.{ Waehrung, CHF, EUR }
import ch.openolitor.stammdaten.StammdatenCommandHandler._
import ch.openolitor.stammdaten.models.Verrechnet
import ch.openolitor.stammdaten.models.Abgeschlossen
import org.joda.time.DateTime

object StammdatenAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenAktionenService = new DefaultStammdatenAktionenService(sysConfig, system)
}

class DefaultStammdatenAktionenService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenAktionenService(sysConfig) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Stammdaten Modul
 */
class StammdatenAktionenService(override val sysConfig: SystemConfig) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with StammdatenDBMappings {
  self: StammdatenWriteRepositoryComponent =>

  val handle: Handle = {
    case LieferplanungAbschliessenEvent(meta, id: LieferplanungId) =>
      lieferplanungAbschliessen(meta, id)
    case LieferplanungAbrechnenEvent(meta, id: LieferplanungId) =>
      lieferplanungVerrechnet(meta, id)
    case BestellungVersendenEvent(meta, id: BestellungId) =>
      bestellungVersenden(meta, id)
    case AuslieferungAlsAusgeliefertMarkierenEvent(meta, id: AuslieferungId) =>
      auslieferungAusgeliefert(meta, id)
    case PasswortGewechseltEvent(meta, personId, pwd) =>
      updatePasswort(meta, personId, pwd)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def lieferplanungAbschliessen(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Offen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Abgeschlossen))
        }
      }
      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        if (Offen == lieferung.status) {
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.copy(status = Abgeschlossen))
        }
      }
      stammdatenWriteRepository.getBestellungen(id) map { bestellung =>
        if (Offen == bestellung.status) {
          stammdatenWriteRepository.updateEntity[Bestellung, BestellungId](bestellung.copy(status = Abgeschlossen))
        }
      }
    }
  }

  def lieferplanungVerrechnet(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Abgeschlossen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Verrechnet))
        }
      }
      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        if (Abgeschlossen == lieferung.status) {
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.copy(status = Verrechnet))
        }
      }
      stammdatenWriteRepository.getBestellungen(id) map { bestellung =>
        if (Abgeschlossen == bestellung.status) {
          stammdatenWriteRepository.updateEntity[Bestellung, BestellungId](bestellung.copy(status = Verrechnet, datumAbrechnung = Some(DateTime.now)))
        }
      }
    }
  }

  def bestellungVersenden(meta: EventMetadata, id: BestellungId)(implicit personId: PersonId = meta.originator) = {
    ???
  }

  def updatePasswort(meta: EventMetadata, id: PersonId, pwd: Array[Char])(implicit personId: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(personMapping, id) map { person =>
        val updated = person.copy(passwort = Some(pwd))
        stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
      }
    }
  }

  def auslieferungAusgeliefert(meta: EventMetadata, id: AuslieferungId)(implicit personId: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(depotAuslieferungMapping, id) map { auslieferung =>
        if (Erfasst == auslieferung.status) {
          stammdatenWriteRepository.updateEntity[DepotAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
        }
      } orElse {
        stammdatenWriteRepository.getById(tourAuslieferungMapping, id) map { auslieferung =>
          if (Erfasst == auslieferung.status) {
            stammdatenWriteRepository.updateEntity[TourAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
          }
        }
      } orElse {
        stammdatenWriteRepository.getById(postAuslieferungMapping, id) map { auslieferung =>
          if (Erfasst == auslieferung.status) {
            stammdatenWriteRepository.updateEntity[PostAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
          }
        }
      }
    }
  }
}
