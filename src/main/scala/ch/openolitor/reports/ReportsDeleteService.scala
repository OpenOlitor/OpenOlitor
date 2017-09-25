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

import akka.actor._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.reports._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.reports.models._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models.PersonId
import ch.openolitor.reports.repositories.DefaultReportsWriteRepositoryComponent
import ch.openolitor.reports.repositories.ReportsWriteRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object ReportsDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): ReportsDeleteService = new DefaultReportsDeleteService(sysConfig, system)
}

class DefaultReportsDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends ReportsDeleteService(sysConfig: SystemConfig) with DefaultReportsWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Reports Modul
 */
class ReportsDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent[_]]
    with LazyLogging with AsyncConnectionPoolContextAware with ReportsDBMappings {
  self: ReportsWriteRepositoryComponent =>
  import EntityStore._

  val handle: Handle = {
    case EntityDeletedEvent(meta, id: ReportId) => deleteReport(meta, id)
    case e =>
  }

  def deleteReport(meta: EventMetadata, id: ReportId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      reportsWriteRepository.deleteEntity[Report, ReportId](id)
    }
  }
}
