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
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import scala.concurrent.duration._
import java.io.InputStream
import org.odftoolkit.simple._
import org.odftoolkit.simple.common.field._
import scala.util._
import spray.json._
import java.io._
import java.nio._

object SingleDocumentReportProcessorActor {
  def props(): Props = Props(classOf[SingleDocumentReportProcessorActor])

  case class GenerateReport(file: Source[ByteString, Unit], data: JsObject)
}

class SingleDocumentReportProcessorActor extends Actor with ActorLogging with DocumentProcessor {
  import SingleDocumentReportProcessorActor._

  val receive: Receive = {
    case GenerateReport(file, data) =>
      sender ! generateReport(file, data)
  }

  private def generateReport(file: Source[ByteString, Unit], data: JsObject): Source[Try[ByteString], Unit] = {
    file.map { stream =>
      for {
        doc <- Try(TextDocument.loadDocument(new ByteBufferBackedInputStream(stream.asByteBuffer)))
        result <- processDocument(doc, data)
      } yield {
        val baos = new ByteArrayOutputStream()
        doc.save(baos)
        ByteString(ByteBuffer.wrap(baos.toByteArray))
      }
    }
  }
}