package ch.openolitor.core.jobs

import akka.actor._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.ws.ClientReceiver
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.AkkaEventStream

object UserJobQueue {
  import JobQueueService._

  def props(personId: PersonId): Props = Props(classOf[UserJobQueue], personId)

  case class JobResultAvailable(personId: PersonId, jobId: JobId, numberOfSuccess: Int, numberOfFailures: Int) extends JSONSerializable
  case class JobFetched(personId: PersonId, jobId: JobId) extends JSONSerializable
}

/**
 * This actor keeps track of pending job and job results per user
 */
class UserJobQueue(personId: PersonId) extends Actor
    with ActorLogging
    with ClientReceiver
    with JobJsonProtocol
    with AkkaEventStream {
  import JobQueueService._
  import UserJobQueue._

  override val system = context.system

  var progressMap = Map[JobId, JobProgress]()
  var jobResults = Map[JobId, JobResult]()

  def receive: Receive = {
    case p: JobProgress =>
      progressMap = progressMap + (p.jobId -> p)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
    case r: JobResult =>
      progressMap = progressMap - (r.jobId)
      jobResults = jobResults + (r.jobId -> r)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
      send(personId, JobResultAvailable(personId, r.jobId, r.numberOfSuccess, r.numberOfFailures))
    case _: GetPendingJobs =>
      sender ! PendingJobs(personId, progressMap.values.toSeq)
    case r: FetchJobResult =>
      jobResults.get(r.jobId) map { result =>
        jobResults = jobResults - (r.jobId)
        sender ! result

        // notify other users session that job result was already fetched
        send(personId, JobFetched(personId, r.jobId))
      } getOrElse {
        sender ! JobResultUnavailable(personId, r.jobId)
      }
  }
}