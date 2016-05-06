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
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.{ Waehrung, CHF, EUR }
import ch.openolitor.stammdaten.StammdatenCommandHandler.LieferplanungAbschliessenEvent
import ch.openolitor.stammdaten.StammdatenCommandHandler.LieferplanungAbrechnenEvent
import ch.openolitor.stammdaten.StammdatenCommandHandler.LieferungBestellenEvent
import ch.openolitor.stammdaten.models.Verrechnet
import ch.openolitor.stammdaten.models.Abgeschlossen

object StammdatenAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenAktionenService = new DefaultStammdatenAktionenService(sysConfig, system)
}

class DefaultStammdatenAktionenService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenAktionenService(sysConfig) with DefaultStammdatenWriteRepositoryComponent with DefaultStammdatenReadRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Stammdaten Modul
 */
class StammdatenAktionenService(override val sysConfig: SystemConfig) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with StammdatenDBMappings {
  self: StammdatenWriteRepositoryComponent with StammdatenReadRepositoryComponent =>

  val handle: Handle = {
    case LieferplanungAbschliessenEvent(meta, id: LieferplanungId) =>
      lieferplanungAbschliessen(meta, id)
    case LieferplanungAbrechnenEvent(meta, id: LieferplanungId) =>
      lieferplanungVerrechnet(meta, id)
    case LieferungBestellenEvent(meta, id: LieferungId) =>
      lieferungBestellen(meta, id)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def lieferplanungAbschliessen(meta: EventMetadata, id: LieferplanungId)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Offen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Abgeschlossen))
        }
      }
    }
    DB autoCommit { implicit session =>
      stammdatenReadRepository.getLieferungen(id) map { lieferungen =>
        lieferungen map { lieferung =>
          if (Offen == lieferung.status) {
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.copy(status = Abgeschlossen))
          }
        }
      }
    }
  }

  def lieferplanungVerrechnet(meta: EventMetadata, id: LieferplanungId)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Abgeschlossen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Verrechnet))
        }
      }
    }
    DB autoCommit { implicit session =>
      stammdatenReadRepository.getLieferungen(id) map { lieferungen =>
        lieferungen map { lieferung =>
          if (Abgeschlossen == lieferung.status) {
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.copy(status = Verrechnet))
          }
        }
      }
    }
  }

  def lieferungBestellen(meta: EventMetadata, id: LieferungId)(implicit userId: UserId = meta.originator) = {
    ???
  }
}
