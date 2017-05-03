package ch.openolitor.core.jobs

import akka.actor._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.ws.ClientReceiver
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.AkkaEventStream
import ch.openolitor.core.ws.ClientMessages.ClientMessage

object UserJobQueue {
  import JobQueueService._

  def props(personId: PersonId): Props = Props(classOf[UserJobQueue], personId)

  case class JobResultAvailable(personId: PersonId, jobId: JobId, numberOfSuccess: Int, numberOfFailures: Int) extends JSONSerializable with ClientMessage
  case class JobFetched(personId: PersonId, jobId: JobId) extends JSONSerializable with ClientMessage
}

/**
 * This actor keeps track of pending job and job results per user
 */
class UserJobQueue(personId: PersonId) extends Actor
    with ActorLogging
    with ClientReceiver
    with JobQueueJsonProtocol
    with AkkaEventStream {
  import JobQueueService._
  import UserJobQueue._

  override val system = context.system

  var progressMap = Map[JobId, JobProgress]()
  var jobResults = Map[JobId, JobResult]()

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
      jobResults = jobResults + (r.jobId -> r)
      send(personId, PendingJobs(personId, progressMap.values.toSeq))
      send(personId, JobResultAvailable(personId, r.jobId, r.numberOfSuccess, r.numberOfFailures))
    case _: GetPendingJobs =>
      sender ! PendingJobs(personId, progressMap.values.toSeq)
    case r: FetchJobResult =>
      jobResults.find(_._1.id == r.jobId) map {
        case (id, result) =>
          jobResults = jobResults - (id)
          sender ! result

          // notify other users session that job result was already fetched
          send(personId, JobFetched(personId, id))
      } getOrElse {
        sender ! JobResultUnavailable(personId, r.jobId)
      }
  }
}