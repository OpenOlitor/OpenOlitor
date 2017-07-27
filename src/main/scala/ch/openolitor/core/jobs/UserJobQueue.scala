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
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.ws.ClientReceiver
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.AkkaEventStream
import ch.openolitor.core.ws.ClientMessages.ClientMessage
import ch.openolitor.core.JSONSerializable
import com.github.blemale.scaffeine._
import ch.openolitor.core.MandantConfiguration
import ch.openolitor.util.ConfigUtil._
import scala.concurrent.duration._
import com.github.benmanes.caffeine.cache.RemovalCause

object UserJobQueue {
  import JobQueueService._

  def props(personId: PersonId, mandantConfiguration: MandantConfiguration): Props = Props(classOf[UserJobQueue], personId, mandantConfiguration)
}

/**
 * This actor keeps track of pending job and job results per user
 */
class UserJobQueue(personId: PersonId, mandantConfiguration: MandantConfiguration) extends Actor
    with ActorLogging
    with ClientReceiver
    with JobQueueJsonProtocol
    with AkkaEventStream {
  import JobQueueService._
  import UserJobQueue._

  override val system = context.system

  val maxJobResults = mandantConfiguration.config.getIntOption("jobqueue.max_results").getOrElse(50)
  val expiresAfterHours = mandantConfiguration.config.getIntOption("jobqueue.expires_after_hours").getOrElse(24)
  val expiresAfterAccessHours = mandantConfiguration.config.getIntOption("jobqueue.expires_after_access_hours").getOrElse(4)

  var progressMap = Map[JobId, JobProgress]()

  private def cleanupResult(result: JobResult) = {
    result.payload match {
      case Some(FileResultPayload(_, _, file)) =>
        // delete temporary file when result gets automatically discarded
        file.delete
      case _ =>
      // nothing to cleanup
    }
  }

  val jobResults: Cache[JobId, JobResult] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(expiresAfterHours.hours)
      .maximumSize(maxJobResults)
      .expireAfterAccess(expiresAfterAccessHours.hours)
      .removalListener { (key: JobId, value: JobResult, cause) =>
        cause match {
          case RemovalCause.EXPIRED =>
            cleanupResult(value)
          case RemovalCause.SIZE =>
            cleanupResult(value)
          case _ => // do nothing
        }
      }
      .build[JobId, JobResult]()

  def receive: Receive = {
    case p: JobProgress if p.numberOfTasksInProgress > 0 =>
      log.debug(s"Received JobProgress:$p")
      progressMap = progressMap + (p.jobId -> p)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
    case r: JobResult if r.payload isEmpty =>
      log.debug(s"Received JobResult without payload:$r")
      progressMap = progressMap - (r.jobId)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
    case r: JobResult =>
      log.debug(s"Received JobResult:$r")
      progressMap = progressMap - (r.jobId)
      jobResults.put(r.jobId, r)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
      send(personId, PendingJobResults(jobResults.asMap.values.map(_.toNotificatication).toSeq))
    case _: GetPendingJobs =>
      sender ! PendingJobs(personId, progressMap.values.toSeq)
    case _: GetPendingJobResults =>
      sender ! PendingJobResults(jobResults.asMap.values.map(_.toNotificatication).toSeq)
    case r: FetchJobResult =>
      jobResults.asMap.find(_._1.id == r.jobId) map {
        case (id, result) =>
          sender ! result

          // notify other users session that job result was already fetched
          send(personId, JobFetched(personId, id))

          jobResults.invalidate(id)
      } getOrElse {
        sender ! JobResultUnavailable(personId, r.jobId)
      }
  }
}