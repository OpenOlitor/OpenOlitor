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

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.EntityStoreReference
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.core.filestore.FileStoreFileId
import ch.openolitor.core.filestore.GeneriertRechnung
import ch.openolitor.core.filestore.GeneriertMahnung

object BuchhaltungReportEventListener extends DefaultJsonProtocol {
  def props(entityStore: ActorRef): Props = Props(classOf[DefaultBuchhaltungReportEventListener], entityStore)
}

class DefaultBuchhaltungReportEventListener(override val entityStore: ActorRef) extends BuchhaltungReportEventListener

/**
 * Listen on Report results to persist ids
 */
abstract class BuchhaltungReportEventListener extends Actor with ActorLogging with BuchhaltungDBMappings with EntityStoreReference {
  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[SingleReportResult])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[SingleReportResult])
    super.postStop()
  }

  val receive: Receive = {
    case SingleReportResult(id: RechnungId, stats, Right(StoredPdfReportResult(_, fileType, fileStoreId))) if fileType == GeneriertRechnung =>
      entityStore ! BuchhaltungCommandHandler.RechnungPDFStoredCommand(stats.originator, id, fileStoreId.id)
    case SingleReportResult(id: RechnungId, stats, Right(StoredPdfReportResult(_, fileType, fileStoreId))) if fileType == GeneriertMahnung =>
      entityStore ! BuchhaltungCommandHandler.MahnungPDFStoredCommand(stats.originator, id, fileStoreId.id)
    case x =>
  }
}
