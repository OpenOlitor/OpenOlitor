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
package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.filestore._
import ch.openolitor.core.filestore.FileStoreActor.StoreFile
import java.util.UUID
import java.util.Locale

object SingleDocumentStoreReportPDFProcessorActor {
  def props(fileStore: FileStore, sysConfig: SystemConfig, fileType: FileType, id: Option[String], name: String, locale: Locale): Props = Props(classOf[SingleDocumentStoreReportPDFProcessorActor], fileStore, sysConfig, fileType, id, name, locale)
}

/**
 * This actor generates a report document, converts it to pdf and stores the pdf in the filestore
 */
class SingleDocumentStoreReportPDFProcessorActor(fileStore: FileStore, sysConfig: SystemConfig, fileType: FileType, idOpt: Option[String], name: String, locale: Locale) extends Actor with ActorLogging {
  import ReportSystem._

  val generatePdfActor = context.actorOf(SingleDocumentReportPDFProcessorActor.props(sysConfig, name, locale), "generate-pdf-" + System.currentTimeMillis)
  val fileStoreActor = context.actorOf(FileStoreActor.props(fileStore), "file-store-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None
  var id: Any = null

  val receive: Receive = {
    case cmd: GenerateReport =>
      origSender = Some(sender)
      id = cmd.id
      generatePdfActor ! cmd
      context become waitingForDocumentResult
  }

  val waitingForDocumentResult: Receive = {
    case PdfReportResult(id, result, name) =>
      fileStoreActor ! StoreFile(fileType.bucket, Some(name), FileStoreFileMetadata(name, fileType), result)
      context become waitigForStoreCompleted
    case e: ReportError =>
      origSender map (_ ! e)
      self ! PoisonPill
  }

  val waitigForStoreCompleted: Receive = {
    case FileStoreError(message) =>
      origSender map (_ ! ReportError(Some(id), message))
      self ! PoisonPill
    case FileStoreFileMetadata(name, _) =>
      origSender map (_ ! StoredPdfReportResult(id, fileType, FileStoreFileId(name)))
      self ! PoisonPill

  }
}
