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
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.PersonId
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungWriteRepositoryComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungWriteRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object BuchhaltungUpdateService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungUpdateService = new DefaultBuchhaltungUpdateService(sysConfig, system)
}

class DefaultBuchhaltungUpdateService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungUpdateService(sysConfig) with DefaultBuchhaltungWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Buchhaltung Moduls
 */
class BuchhaltungUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware with BuchhaltungDBMappings {
  self: BuchhaltungWriteRepositoryComponent =>

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: RechnungId, entity: RechnungModify) => updateRechnung(meta, id, entity)
    case EntityUpdatedEvent(meta, id: RechnungsPositionId, entity: RechnungsPositionModify) => updateRechnungsPosition(meta, id, entity)
    case EntityUpdatedEvent(meta, id: RechnungsPositionId, entity: RechnungsPositionAssignToRechnung) => assignRechnungsPositionToRechnung(meta, id, entity)
    case e =>
  }

  def assignRechnungsPositionToRechnung(meta: EventMetadata, id: RechnungsPositionId, update: RechnungsPositionAssignToRechnung)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.updateEntity(id)(
        rechnungsPositionMapping.column.rechnungId -> Option(update.rechnungId),
        rechnungsPositionMapping.column.status -> RechnungsPositionStatus.Zugewiesen,
        rechnungsPositionMapping.column.sort -> Option(update.sort)
      )
    }
  }

  def updateRechnung(meta: EventMetadata, id: RechnungId, update: RechnungModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.getById(rechnungMapping, id) map { entity =>
        //map all updatable fields
        val copy = copyFrom(entity, update)
        buchhaltungWriteRepository.updateEntityFully[Rechnung, RechnungId](copy)
      }
    }
  }

  def updateRechnungsPosition(meta: EventMetadata, id: RechnungsPositionId, update: RechnungsPositionModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.updateEntity(id)(
        rechnungsPositionMapping.column.beschrieb -> update.beschrieb,
        rechnungsPositionMapping.column.anzahlLieferungen -> update.anzahlLieferungen,
        rechnungsPositionMapping.column.betrag -> update.betrag,
        rechnungsPositionMapping.column.status -> update.status
      )
    }
  }
}
