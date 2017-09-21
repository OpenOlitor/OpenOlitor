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
import ch.openolitor.core.jobs.JobQueueService
import spray.http.MediaTypes
import ch.openolitor.core.DateFormats
import ch.openolitor.core.jobs.JobQueueService.FileResultPayload
import java.io.File
import java.io.FileOutputStream
import ch.openolitor.util.ZipBuilderWithFile

object ZipReportResultCollector {
  def props(reportSystem: ActorRef, jobQueueService: ActorRef): Props = Props(classOf[ZipReportResultCollector], reportSystem, jobQueueService)
}

/**
 * Collect all results into a zip file. Send back the zip result when all reports got generated.
 * This ResultCollector stores the generated documents in a local zip which will eventually cause out of disk space errors.
 */
class ZipReportResultCollector(reportSystem: ActorRef, override val jobQueueService: ActorRef) extends ResultCollector with DateFormats {

  var origSender: Option[ActorRef] = None
  val zipBuilder: ZipBuilder = new ZipBuilderWithFile()
  var errors: Seq[ReportError] = Seq()

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, stats, Left(error)) =>
      errors = errors :+ error
      notifyProgress(stats)
    case SingleReportResult(id, stats, Right(result: ReportResultWithDocument)) =>
      log.debug(s"Add Zip Entry:${result.name}")
      zipBuilder.addZipEntry(result.name, result.document) match {
        case Success(r) =>
          result.document.delete()
        case Failure(error) =>
          log.warning(s"Coulnd't att document to  zip file:$error")
          errors = errors :+ ReportError(Some(id), s"Dokument konnte nicht zum Zip hinzugefügt werde:$error")
      }
      notifyProgress(stats)
    case result: GenerateReportsStats if result.numberOfReportsInProgress == 0 =>
      log.debug(s"Close Zip, job finished:${result}")
      //finished, send back zip result
      zipBuilder.close() map { zip =>
        val fileName = "Report_" + filenameDateFormat.print(System.currentTimeMillis()) + ".zip"
        val payload = FileResultPayload(fileName, MediaTypes.`application/zip`, zip)
        log.debug(s"Send payload as result:${fileName}")
        jobFinished(result, Some(payload))
      }
      log.debug(s"Stop collector PoisonPill")
      self ! PoisonPill
    case stats: GenerateReportsStats =>
      notifyProgress(stats)
  }
}