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
package ch.openolitor.core.reporting.pdf

import akka.actor._
import akka.util.ByteString
import scala.concurrent.blocking
import scala.util._

object PDFGeneratorActor {
  def props(): Props = Props(classOf[PDFGeneratorActor])

  case class GeneratePDF(document: ByteString)
  case class PDFResult(pdf: ByteString)
  case class PDFError(error: String)
}

class PDFGeneratorActor extends Actor with ActorLogging with PDFGeneratorService {
  import PDFGeneratorActor._

  val receive: Receive = {
    case GeneratePDF(document) =>
      val rec = sender
      blocking {
        //run pdf service in blocking mode, only one pdf can get generated once
        generatePDF(document) match {
          case Success(result) => rec ! PDFResult(result)
          case Failure(error) =>
            log.warning(s"Failed converting pdf {}", error)
            rec ! PDFError(error.getMessage)
        }

        //kill outself
        self ! PoisonPill
      }
  }
}