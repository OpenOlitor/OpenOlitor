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

import scala.util.Try

import scala.util.{ Success, Failure }
import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import spray.http._
import spray.client.pipelining._
import spray.util._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorRefFactory
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import akka.io.IO
import spray.can.server.UHttp
import java.io.File
import ch.openolitor.util.FileUtil._

trait PDFGeneratorService {
  def sysConfig: SystemConfig

  lazy val endpointUri = sysConfig.mandantConfiguration.config.getString("converttopdf.endpoint")

  implicit val system: ActorSystem
  import system.dispatcher

  def uSendReceive(implicit refFactory: ActorRefFactory, executionContext: ExecutionContext,
    futureTimeout: Timeout = 600.seconds): SendReceive =
    sendReceive(IO(UHttp)(actorSystem))

  val pipeline: HttpRequest => Future[HttpResponse] = uSendReceive

  def generatePDF(input: File, name: String): Try[File] = synchronized {
    Try {
      val uri = Uri(endpointUri)
      val formFile = FormFile(Some(name + ".odt"), HttpEntity(HttpData(input)).asInstanceOf[HttpEntity.NonEmpty])
      val formData = MultipartFormData(Seq(BodyPart(formFile, "upload"), BodyPart(HttpEntity(name + ".odt"), "name")))

      val result = pipeline(Post(uri, formData)) map {
        case HttpResponse(StatusCodes.OK, entity, headers, _) =>
          entity.data.toByteArray.asTempFile("report_pdf", ".pdf")
        case other =>
          throw new RequestProcessingException(StatusCodes.InternalServerError, s"PDF konnte nicht generiert werden ${other}")
      }

      Await.result(result, 600 seconds)
    }
  }
}
