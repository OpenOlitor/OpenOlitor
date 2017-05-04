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
import java.util.Locale
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.models._
import ch.openolitor.core.jobs.JobQueueService.JobId
import java.io.File

object ReportSystem {
  def props(fileStore: FileStore, sysConfig: SystemConfig): Props = Props(classOf[ReportSystem], fileStore, sysConfig)

  case class ReportDataRow(id: Any, value: JsObject, fileStoreId: Option[String], name: String, locale: Locale)
  case class ReportData[E: JsonFormat](rowsRaw: Seq[E], idFactory: E => Any, filestoreIdFactory: E => Option[String], nameFactory: E => String, localeFactory: E => Locale) {
    val rows = rowsRaw.map(row => ReportDataRow(idFactory(row), row.toJson.asJsObject, filestoreIdFactory(row), nameFactory(row), localeFactory(row)))
  }

  trait ReportResult {
  }
  trait ReportResultWithId extends ReportResult {
    val id: Any
  }
  trait ReportResultWithDocument {
    val name: String
    val document: File
  }
  trait ReportSuccess extends ReportResultWithId
  case class ReportDataResult(id: Any, data: JsArray) extends ReportSuccess
  case class AsyncReportResult(jobId: JobId) extends ReportResult
  case class DocumentReportResult(id: Any, document: File, name: String) extends ReportSuccess with ReportResultWithDocument
  case class PdfReportResult(id: Any, document: File, name: String) extends ReportSuccess with ReportResultWithDocument
  case class StoredPdfReportResult(id: Any, fileType: FileType, fileStoreId: FileStoreFileId) extends ReportSuccess with JSONSerializable
  case class ReportError(id: Option[Any], error: String) extends ReportResultWithId with JSONSerializable

  case class FileStoreParameters[E](fileType: FileType)
  case class GenerateReports[E](originator: PersonId, jobId: JobId, file: Array[Byte], data: ReportData[E], pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]])
  case class GenerateReport(id: Any, file: Array[Byte], data: JsObject)
  case class SingleReportResult(id: Any, stats: GenerateReportsStats, result: Either[ReportError, ReportResultWithId]) extends ReportResultWithId
  case class ZipReportResult(stats: GenerateReportsStats, errors: Seq[ReportError], results: Option[Array[Byte]]) extends ReportResult
  case class BatchStoredPdfReportResult(stats: GenerateReportsStats, errors: Seq[ReportError], results: Seq[FileStoreFileReference]) extends ReportResult with JSONSerializable
  case class GenerateReportsStats(originator: PersonId, jobId: JobId, numberOfReportsInProgress: Int, numberOfSuccess: Int, numberOfFailures: Int) extends ReportResult
      with JSONSerializable {
    def incSuccess: GenerateReportsStats =
      copy(numberOfSuccess = this.numberOfSuccess + 1, numberOfReportsInProgress = this.numberOfReportsInProgress - 1)
    def incError: GenerateReportsStats =
      copy(numberOfSuccess = this.numberOfFailures + 1, numberOfReportsInProgress = this.numberOfReportsInProgress - 1)
    def isFinished: Boolean = numberOfReportsInProgress <= 0
  }
}

/**
 * The reportsystem is responsible to dispatch report generating request to processor actors
 */
class ReportSystem(fileStore: FileStore, sysConfig: SystemConfig) extends Actor with ActorLogging {
  import ReportSystem._

  val receive: Receive = {
    case request: GenerateReports[_] =>
      val processor = context.actorOf(ReportProcessorActor.props(fileStore, sysConfig), "report-processor-" + System.currentTimeMillis)
      //forward request to new processor-actor
      processor forward request
    case x =>
      log.debug(s"Received result:$x")
  }
}