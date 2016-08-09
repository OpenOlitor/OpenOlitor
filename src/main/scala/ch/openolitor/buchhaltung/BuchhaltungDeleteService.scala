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

import akka.persistence.PersistentView
import akka.actor._
import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.buchhaltung._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.buchhaltung.models._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models.PersonId

object BuchhaltungDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungDeleteService = new DefaultBuchhaltungDeleteService(sysConfig, system)
}

class DefaultBuchhaltungDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungDeleteService(sysConfig: SystemConfig) with DefaultBuchhaltungWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Buchhaltung Modul
 */
class BuchhaltungDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent[_]]
    with LazyLogging with AsyncConnectionPoolContextAware with BuchhaltungDBMappings {
  self: BuchhaltungWriteRepositoryComponent =>
  import EntityStore._

  val handle: Handle = {
    case EntityDeletedEvent(meta, id: RechnungId) => deleteRechnung(meta, id)
    case e =>
  }

  def deleteRechnung(meta: EventMetadata, id: RechnungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      buchhaltungWriteRepository.deleteEntity[Rechnung, RechnungId](id, { rechnung: Rechnung => rechnung.status == Erstellt })
    }
  }
}