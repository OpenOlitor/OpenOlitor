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
package ch.openolitor.core.jobs

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core._
import scala.util.Properties
import ch.openolitor.core.security.Subject
import akka.pattern.ask
import ch.openolitor.core.jobs.JobQueueService._
import akka.util.Timeout
import scala.concurrent.ExecutionContext

trait JobQueueRoutes extends HttpService with DefaultRouteService with JobQueueJsonProtocol with JobQueueServiceReference {

  implicit val timeout: Timeout

  def jobQueueRoute(implicit subject: Subject): Route =
    pathPrefix("queue") {
      path("jobs") {
        get {
          onSuccess(jobQueueService ? GetPendingJobs(subject.personId)) {
            case result: PendingJobs =>
              complete(result)
            case x =>
              logger.warn(s"Unexpected result:$x")
              complete(StatusCodes.BadRequest)
          }
        }
      } ~
        path("results") {
          get {
            onSuccess(jobQueueService ? GetPendingJobResults(subject.personId)) {
              case result: PendingJobResults =>
                complete(result)
              case x =>
                logger.warn(s"Unexpected result:$x")
                complete(StatusCodes.BadRequest)
            }
          }
        } ~
        path("job" / Segment) { jobId =>
          get {
            onSuccess(jobQueueService ? FetchJobResult(subject.personId, jobId)) {
              case JobResult(_, _, _, _, Some(result: FileResultPayload)) =>
                streamFile(result.fileName, result.mediaType, result.file, true)
              case JobResult(_, _, _, _, Some(result: FileStoreResultPayload)) if result.fileStoreReferences.size == 1 =>
                val file = result.fileStoreReferences.head
                download(file.fileType, file.id.id)
              case JobResult(_, _, _, _, Some(result: FileStoreResultPayload)) =>
                downloadAsZip("Report_" + filenameDateFormat.print(System.currentTimeMillis()) + ".zip", result.fileStoreReferences)
              case result: JobResultUnavailable =>
                complete(StatusCodes.NotFound, s"No job found for id:${result.jobId}")
              case x =>
                logger.warn(s"Unexpected result:$x")
                complete(StatusCodes.BadRequest)
            }
          }
        }
    }
}