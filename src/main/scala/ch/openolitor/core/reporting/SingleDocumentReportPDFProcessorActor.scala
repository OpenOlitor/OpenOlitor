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
import ch.openolitor.core.reporting.pdf.PDFGeneratorActor

object SingleDocumentReportPDFProcessorActor {
  def props(): Props = Props(classOf[SingleDocumentReportPDFProcessorActor])
}

/**
 * This actor generates a report document and converts the result to a pdf afterwards
 */
class SingleDocumentReportPDFProcessorActor() extends Actor with ActorLogging {
  import ReportSystem._
  import PDFGeneratorActor._

  val generateDocumentActor = context.actorOf(SingleDocumentReportProcessorActor.props)
  val generatePdfActor = context.actorOf(PDFGeneratorActor.props)

  var origSender: Option[ActorRef] = None

  val receive: Receive = {
    case cmd: GenerateReport =>
      origSender = Some(sender)
      generateDocumentActor ! cmd
      context become waitingForDocumentResult
  }

  val waitingForDocumentResult: Receive = {
    case DocumentReportResult(document) =>
      generatePdfActor ! GeneratePDF(document)
      context become waitingForPdfResult
    case e: ReportError =>
      //stop on error
      origSender.map(_ ! e)
      self ! PoisonPill
  }

  val waitingForPdfResult: Receive = {
    case PDFResult(pdf) =>
      origSender.map(_ ! PdfReportResult(pdf))
      self ! PoisonPill
    case PDFError(error) =>
      origSender.map(_ ! ReportError(error))
      self ! PoisonPill
  }
}