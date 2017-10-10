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
package ch.openolitor.reports

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.reports._
import ch.openolitor.reports.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import ch.openolitor.core.Macros._
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.reports.repositories.DefaultReportsWriteRepositoryComponent
import ch.openolitor.reports.repositories.ReportsWriteRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import ch.openolitor.stammdaten.models.KundeId

object ReportsInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): ReportsInsertService = new DefaultReportsInsertService(sysConfig, system)
}

class DefaultReportsInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends ReportsInsertService(sysConfig) with DefaultReportsWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Reports Modul
 */
class ReportsInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware
    with ReportsDBMappings {
  self: ReportsWriteRepositoryComponent =>

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: ReportId, entity: ReportCreate) =>
      createReport(meta, id, entity)
    case e =>
  }

  def createReport(meta: EventMetadata, id: ReportId, entity: ReportCreate)(implicit personId: PersonId = meta.originator): Option[Report] = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>

      val rp = copyTo[ReportCreate, Report](
        entity,
        "id" -> id,
        "erstelldat" -> meta.timestamp,
        "ersteller" -> meta.originator,
        "modifidat" -> meta.timestamp,
        "modifikator" -> meta.originator
      )

      reportsWriteRepository.insertEntity[Report, ReportId](rp)
    }
  }

}
