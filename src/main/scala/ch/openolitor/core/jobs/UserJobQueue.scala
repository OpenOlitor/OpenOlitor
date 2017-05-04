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
    case p: JobProgress =>
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
          jobResults.invalidate(id)
          sender ! result

          // notify other users session that job result was already fetched
          send(personId, JobFetched(personId, id))
      } getOrElse {
        sender ! JobResultUnavailable(personId, r.jobId)
      }
  }
}