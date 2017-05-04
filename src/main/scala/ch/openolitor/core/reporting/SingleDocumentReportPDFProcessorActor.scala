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
import ch.openolitor.core.reporting.pdf.PDFGeneratorActor
import java.util.Locale
import java.io.File

object SingleDocumentReportPDFProcessorActor {
  def props(sysConfig: SystemConfig, name: String, locale: Locale): Props = Props(classOf[SingleDocumentReportPDFProcessorActor], sysConfig, name, locale)
}

/**
 * This actor generates a report document and converts the result to a pdf afterwards
 */
class SingleDocumentReportPDFProcessorActor(sysConfig: SystemConfig, name: String, locale: Locale) extends Actor with ActorLogging {
  import ReportSystem._
  import PDFGeneratorActor._

  val generateDocumentActor = context.actorOf(SingleDocumentReportProcessorActor.props(name, locale), "generate-document-" + System.currentTimeMillis)
  val generatePdfActor = context.actorOf(PDFGeneratorActor.props(sysConfig, name), "pdf-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None
  var id: Any = null
  var odtFile: File = null

  val receive: Receive = {
    case cmd: GenerateReport =>
      origSender = Some(sender)
      generateDocumentActor ! cmd
      context become waitingForDocumentResult
  }

  val waitingForDocumentResult: Receive = {
    case DocumentReportResult(id, document, _) =>
      this.id = id
      odtFile = document
      generatePdfActor ! GeneratePDF(document)
      context become waitingForPdfResult
    case e: ReportError =>
      //stop on error
      origSender map (_ ! e)
      self ! PoisonPill
  }

  override def postStop() = {
    // cleanup
    if (odtFile != null) {
      odtFile.delete()
    }
  }

  val waitingForPdfResult: Receive = {
    case PDFResult(pdf) =>
      origSender map (_ ! PdfReportResult(id, pdf, name + ".pdf"))
      self ! PoisonPill
    case PDFError(error) =>
      origSender map (_ ! ReportError(Some(id), error))
      self ! PoisonPill
  }
}