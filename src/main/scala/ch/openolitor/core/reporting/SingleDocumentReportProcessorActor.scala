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
import akka.util._
import scala.concurrent.duration._
import java.io.InputStream
import org.odftoolkit.simple._
import org.odftoolkit.simple.common.field._
import scala.util._
import spray.json._
import java.io._
import java.nio._
import ch.openolitor.util.ByteBufferBackedInputStream

object SingleDocumentReportProcessorActor {
  def props(): Props = Props(classOf[SingleDocumentReportProcessorActor])
}

/**
 * This generates a single report documet from a given json data object
 */
class SingleDocumentReportProcessorActor extends Actor with ActorLogging with DocumentProcessor {
  import ReportSystem._

  val receive: Receive = {
    case GenerateReport(file, data) =>
      generateReport(file, data) match {
        case Success(result) => {
          sender ! DocumentReportResult(result)
        }
        case Failure(error) => {
          error.printStackTrace()
          log.warning(s"Couldn't generate report document {}", error)
          sender ! ReportError(error.getMessage)
        }
      }
      self ! PoisonPill
  }

  private def generateReport(file: Array[Byte], data: JsObject): Try[Array[Byte]] = {
    for {
      doc <- Try(TextDocument.loadDocument(new ByteArrayInputStream(file)))
      result <- Try(processDocument(doc, data))
    } yield {
      val baos = new ByteArrayOutputStream()
      doc.save(baos)
      baos.toByteArray
    }
  }
}