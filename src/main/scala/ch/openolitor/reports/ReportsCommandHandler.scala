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

import ch.openolitor.core.domain._
import ch.openolitor.reports.models._
import ch.openolitor.core.models._
import scala.util._
import scalikejdbc.DB
import ch.openolitor.reports.models._
import ch.openolitor.core.exceptions.InvalidStateException
import akka.actor.ActorSystem
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.filestore.FileStoreComponent
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import ch.openolitor.core.filestore.ZahlungsImportBucket
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.domain.EntityStore.EntityInsertedEvent
import ch.openolitor.core.filestore.FileStoreBucket
import scala.io.Source
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.concurrent.Future
import ch.openolitor.reports.repositories.DefaultReportsReadRepositorySyncComponent
import ch.openolitor.reports.repositories.ReportsReadRepositorySyncComponent
import org.joda.time.DateTime

object ReportsCommandHandler {

}

trait ReportsCommandHandler extends CommandHandler with ReportsDBMappings with ConnectionPoolContextAware with AsyncConnectionPoolContextAware {
  self: ReportsReadRepositorySyncComponent =>
  import ReportsCommandHandler._
  import EntityStore._

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    /*
     * Insert command handling
     */
    case e @ InsertEntityCommand(personId, entity: ReportCreate) => idFactory => meta =>
      handleEntityInsert[ReportCreate, ReportId](idFactory, meta, entity, ReportId.apply)
  }
}

class DefaultReportsCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends ReportsCommandHandler
    with DefaultReportsReadRepositorySyncComponent {
}
