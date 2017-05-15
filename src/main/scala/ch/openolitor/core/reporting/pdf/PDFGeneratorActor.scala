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
import scala.concurrent.blocking
import scala.util._
import ch.openolitor.core.SystemConfig
import java.io.File

object PDFGeneratorActor {
  def props(sysConfig: SystemConfig, name: String): Props = Props(classOf[PDFGeneratorActor], sysConfig, name)

  case class GeneratePDF(document: File)
  case class PDFResult(pdf: File)
  case class PDFError(error: String)
}

class PDFGeneratorActor(override val sysConfig: SystemConfig, name: String) extends Actor with ActorLogging with PDFGeneratorService {
  import PDFGeneratorActor._

  override lazy val system = context.system

  val receive: Receive = {
    case GeneratePDF(document) =>
      val rec = sender
      blocking {
        //run pdf service in blocking mode, only one pdf can get generated once
        generatePDF(document, name) match {
          case Success(result) => rec ! PDFResult(result)
          case Failure(error) =>
            log.warning(s"Failed converting pdf {}", error)
            rec ! PDFError(error.getMessage)
        }

        //kill ourself
        self ! PoisonPill
      }
  }
}