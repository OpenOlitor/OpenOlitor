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