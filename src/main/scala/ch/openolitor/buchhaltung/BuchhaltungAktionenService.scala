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
package ch.openolitor.buchhaltung

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
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
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler.RechnungVerschicktEvent
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler.RechnungMahnungVerschicktEvent
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler.RechnungBezahltEvent
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler.RechnungStorniertEvent
import ch.openolitor.buchhaltung.models.RechnungModifyBezahlt

object BuchhaltungAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungAktionenService = new DefaultBuchhaltungAktionenService(sysConfig, system)
}

class DefaultBuchhaltungAktionenService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungAktionenService(sysConfig) with DefaultBuchhaltungRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Buchhaltung Modul
 */
class BuchhaltungAktionenService(override val sysConfig: SystemConfig) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings {
  self: BuchhaltungRepositoryComponent =>

  val handle: Handle = {
    case RechnungVerschicktEvent(meta, id: RechnungId) =>
      rechnungVerschicken(meta, id)
    case RechnungMahnungVerschicktEvent(meta, id: RechnungId) =>
      rechnungMahnungVerschicken(meta, id)
    case RechnungBezahltEvent(meta, id: RechnungId, entity: RechnungModifyBezahlt) =>
      rechnungBezahlen(meta, id, entity)
    case RechnungStorniertEvent(meta, id: RechnungId) =>
      rechnungStornieren(meta, id)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def rechnungVerschicken(meta: EventMetadata, id: RechnungId)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(rechnungMapping, id) map { rechnung =>
        if (Erstellt == rechnung.status) {
          writeRepository.updateEntity[Rechnung, RechnungId](rechnung.copy(status = Verschickt))
        }
      }
    }
  }

  def rechnungMahnungVerschicken(meta: EventMetadata, id: RechnungId)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(rechnungMapping, id) map { rechnung =>
        if (Verschickt == rechnung.status) {
          writeRepository.updateEntity[Rechnung, RechnungId](rechnung.copy(status = MahnungVerschickt))
        }
      }
    }
  }

  def rechnungBezahlen(meta: EventMetadata, id: RechnungId, entity: RechnungModifyBezahlt)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(rechnungMapping, id) map { rechnung =>
        if (Verschickt == rechnung.status || MahnungVerschickt == rechnung.status) {
          writeRepository.updateEntity[Rechnung, RechnungId](rechnung.copy(
            einbezahlterBetrag = Some(entity.einbezahlterBetrag),
            eingangsDatum = Some(entity.eingangsDatum),
            status = Bezahlt
          ))
        }
      }
    }
  }

  def rechnungStornieren(meta: EventMetadata, id: RechnungId)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(rechnungMapping, id) map { rechnung =>
        if (Bezahlt != rechnung.status) {
          writeRepository.updateEntity[Rechnung, RechnungId](rechnung.copy(status = Storniert))
        }
      }
    }
  }
}
