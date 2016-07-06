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

object FileStoreReportResultCollector {
  def props(reportSystem: ActorRef): Props = Props(classOf[FileStoreReportResultCollector], reportSystem)
}

/**
 * Collect all results filestore id results
 */
class FileStoreReportResultCollector(reportSystem: ActorRef) extends Actor with ActorLogging {

  var origSender: Option[ActorRef] = None
  var storeResults: Seq[FileStoreFileReference] = Seq()
  var errors: Seq[ReportError] = Seq()

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, _, Left(error)) =>
      errors = errors :+ error
    case SingleReportResult(_, _, Right(StoredPdfReportResult(_, fileType, id))) =>
      storeResults = storeResults :+ FileStoreFileReference(fileType, id)
    case result: GenerateReportsStats =>
      //finished, send back collected result
      origSender map (_ ! BatchStoredPdfReportResult(result, errors, storeResults))
      self ! PoisonPill
  }
}