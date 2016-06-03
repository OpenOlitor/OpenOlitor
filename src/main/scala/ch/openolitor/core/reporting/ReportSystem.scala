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
import spray.json._
import ch.openolitor.core.filestore._
import java.util.zip.ZipFile

object ReportSystem {
  def props(fileStore: FileStore): Props = Props(classOf[ReportSystem], fileStore)

  case class JobId(id: Long = System.currentTimeMillis)
  case class ReportData[E: JsonFormat](jobId: JobId, rows: Seq[E]) {
    val rowsAsJson = rows.map(_.toJson.asJsObject)
  }

  trait ReportResult
  trait ReportSuccess extends ReportResult
  case class DocumentReportResult(document: Array[Byte]) extends ReportSuccess
  case class PdfReportResult(document: Array[Byte]) extends ReportSuccess
  case class StoredPdfReportResult(fileType: FileType, id: FileStoreFileId) extends ReportSuccess
  case class ReportError(error: String) extends ReportResult

  case class FileStoreParameters[E](fileType: FileType, idFactory: E => Option[String], nameFactory: E => String)
  case class GenerateReports[E](file: Array[Byte], data: ReportData[E], pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]])
  case class GenerateReport(file: Array[Byte], data: JsObject)
  case class SingleReportResult(stats: GenerateReportsStats, result: Either[ReportError, ReportResult]) extends ReportResult
  case class ZipReportResult(stats: GenerateReportsStats, errors: Seq[ReportError], results: Option[ZipFile]) extends ReportResult
  case class GenerateReportsStats(jobId: Option[JobId], numberOfReportsInProgress: Int, numberOfSuccess: Int, numberOfFailures: Int) extends ReportResult
}

/**
 * The reportsystem is responsible to dispatch report generating request to processor actors
 */
class ReportSystem(fileStore: FileStore) extends Actor with ActorLogging {
  import ReportSystem._

  val receive: Receive = {
    case request: GenerateReports[_] =>
      log.debug(s"Generate report from:" + sender)
      val processor = context.actorOf(ReportProcessorActor.props(fileStore), "report-processor-" + System.currentTimeMillis)
      //forward request to new processor-actor
      processor forward request
    case x =>
      log.debug(s"Received result:$x")
  }
}