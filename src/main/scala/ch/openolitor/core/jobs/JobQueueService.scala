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

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.JSONSerializable
import java.util.UUID
import java.io.File
import org.joda.time.DateTime
import spray.http.MediaType
import ch.openolitor.core.filestore.FileType
import ch.openolitor.core.filestore.FileStoreFileId
import ch.openolitor.core.filestore.FileStoreFileReference
import ch.openolitor.core.ws.ClientMessages.ClientMessage
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.MandantConfiguration

object JobQueueService {
  def props(mandantConfiguration: MandantConfiguration): Props = Props(classOf[JobQueueService], mandantConfiguration: MandantConfiguration)

  case class JobId(name: String, id: String = UUID.randomUUID().toString, startTime: DateTime = DateTime.now) extends JSONSerializable
  case class GetPendingJobs(personId: PersonId) extends PersonReference
  case class GetPendingJobResults(personId: PersonId) extends PersonReference
  case class FetchJobResult(personId: PersonId, jobId: String) extends PersonReference
  case class PendingJobs(personId: PersonId, progresses: Seq[JobProgress]) extends JSONSerializable with ClientMessage
  case class PendingJobResults(results: Seq[JobResultAvailable]) extends JSONSerializable with ClientMessage
  case class JobResultAvailable(personId: PersonId, jobId: JobId, numberOfSuccess: Int, numberOfFailures: Int) extends JSONSerializable
  case class JobFetched(personId: PersonId, jobId: JobId) extends JSONSerializable with ClientMessage

  sealed trait ResultPayload extends JSONSerializable
  case class FileResultPayload(fileName: String, mediaType: MediaType, file: File) extends ResultPayload
  case class FileStoreResultPayload(fileStoreReferences: Seq[FileStoreFileReference]) extends ResultPayload

  case class JobResult(personId: PersonId, jobId: JobId, numberOfSuccess: Int, numberOfFailures: Int, payload: Option[ResultPayload]) extends PersonReference with ClientMessage {
    def toNotificatication =
      JobResultAvailable(personId, jobId, numberOfSuccess, numberOfFailures)
  }
  case class JobResultUnavailable(personId: PersonId, jobId: String)
  case class JobProgress(personId: PersonId, jobId: JobId, numberOfTasksInProgress: Int, numberOfSuccess: Int, numberOfFailures: Int)
    extends JSONSerializable with PersonReference
}

/**
 * This job queue service provides access to the user based job queue
 */
class JobQueueService(mandantConfiguration: MandantConfiguration) extends Actor with ActorLogging {

  import JobQueueService._

  /**
   * Implicit convertion from personid object model to string based representation used in akka system
   */
  implicit def userId2String(id: PersonId): String = id.id.toString

  def receive: Receive = {
    case cmd: PersonReference =>
      child(cmd.personId) forward cmd
  }

  protected def child(id: PersonId): ActorRef =
    context.child(id) getOrElse create(id)

  protected def create(id: PersonId): ActorRef = {
    val agg = context.actorOf(childProps(id), id)
    context watch agg
    agg
  }

  def childProps(id: PersonId): Props = UserJobQueue.props(id, mandantConfiguration)
}