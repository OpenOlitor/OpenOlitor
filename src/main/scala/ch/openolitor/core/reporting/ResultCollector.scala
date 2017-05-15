package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.core.JobQueueServiceReference
import ch.openolitor.core.reporting.ReportSystem._
import spray.http.MediaType
import java.io.File
import ch.openolitor.core.jobs.JobQueueService._
import ch.openolitor.core.filestore.FileType
import ch.openolitor.core.filestore.FileStoreFileId

/**
 *
 */
trait ResultCollector extends Actor with ActorLogging with JobQueueServiceReference {
  /**
   * Notify jobqueue when a job is finished with an optional payload
   */
  protected def jobFinished(stats: GenerateReportsStats, payload: Option[ResultPayload]) = {
    val jobResult = JobResult(
      personId = stats.originator,
      jobId = stats.jobId,
      numberOfSuccess = stats.numberOfSuccess,
      numberOfFailures = stats.numberOfFailures,
      payload = payload
    )
    jobQueueService ! jobResult
  }

  /**
   * Notify an updated job progress
   */
  protected def notifyProgress(stats: GenerateReportsStats) = {
    val progress = JobProgress(
      personId = stats.originator,
      jobId = stats.jobId,
      numberOfTasksInProgress = stats.numberOfReportsInProgress,
      numberOfSuccess = stats.numberOfSuccess,
      numberOfFailures = stats.numberOfFailures
    )
    jobQueueService ! progress
  }
}