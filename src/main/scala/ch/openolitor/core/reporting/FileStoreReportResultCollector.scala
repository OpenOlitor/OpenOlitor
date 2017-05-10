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
import ch.openolitor.core.reporting.ReportSystem._
import java.io.ByteArrayOutputStream
import scala.util._
import ch.openolitor.util.ZipBuilder
import ch.openolitor.core.filestore.FileStoreFileReference
import ch.openolitor.core.jobs.JobQueueService
import ch.openolitor.core.jobs.JobQueueService.FileStoreResultPayload

object FileStoreReportResultCollector {
  def props(reportSystem: ActorRef, jobQueueService: ActorRef, downloadFile: Boolean): Props = Props(classOf[FileStoreReportResultCollector], reportSystem, jobQueueService, downloadFile)
}

/**
 * Collect all results filestore id results
 */
class FileStoreReportResultCollector(reportSystem: ActorRef, override val jobQueueService: ActorRef, downloadFile: Boolean) extends ResultCollector {

  var storeResults: Seq[FileStoreFileReference] = Seq()
  var errors: Seq[ReportError] = Seq()

  val receive: Receive = {
    case request: GenerateReports[_] =>
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, stats, Left(error)) =>
      log.debug(s"Received error:${error}:$stats")
      errors = errors :+ error
      notifyProgress(stats)
    case SingleReportResult(_, stats, Right(StoredPdfReportResult(_, fileType, id))) =>
      log.debug(s"Received resukt:${id}:$stats")
      storeResults = storeResults :+ FileStoreFileReference(fileType, id)
      notifyProgress(stats)
    case result: GenerateReportsStats if result.numberOfReportsInProgress == 0 =>
      log.debug(s"Job finished: $result, downloadFile:$downloadFile")
      //finished, send collected result to jobQueue      
      if (downloadFile) {
        val payload = FileStoreResultPayload(storeResults)
        jobFinished(result, Some(payload))
      } else {
        jobFinished(result, None)
      }
      self ! PoisonPill
    case stats: GenerateReportsStats =>
      notifyProgress(stats)
  }
}