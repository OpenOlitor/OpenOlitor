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
import ch.openolitor.core.reporting.ReportSystem._
import scala.util._
import ch.openolitor.util.ZipBuilder
import ch.openolitor.core.jobs.JobQueueService
import spray.http.MediaTypes
import ch.openolitor.core.DateFormats
import ch.openolitor.core.jobs.JobQueueService.FileResultPayload
import ch.openolitor.core.filestore._
import java.io.FileInputStream
import ch.openolitor.core.jobs.JobQueueService.FileStoreResultPayload
import java.util.zip.ZipOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.PipedOutputStream
import java.io.PipedInputStream
import java.io.InputStream
import scala.concurrent.Future
import akka.stream.scaladsl.StreamConverters
import akka.pattern.ask
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import scala.concurrent.duration._
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import ch.openolitor.core.reporting.BufferedChunkingActor.CompleteChunkedTransfer
import ch.openolitor.core.filestore.ChunkedFileStoreActor.InitiateChunkedUpload

object ChunkedReportResultCollector {
  def props(fileStore: FileStore, fileName: String, reportSystem: ActorRef, jobQueueService: ActorRef): Props = Props(classOf[ChunkedReportResultCollector], fileStore, fileName, reportSystem, jobQueueService)
}

/**
 * Collect all results into a remote zip file on the file store.
 */
class ChunkedReportResultCollector(fileStore: FileStore, fileName: String, reportSystem: ActorRef, override val jobQueueService: ActorRef) extends ResultCollector with DateFormats {
  val bufferedChunkingActor = context.actorOf(BufferedChunkingActor.props(fileStore, fileName, 10 * 1024 * 1024), "buffered-chunking-actor-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None
  var errors: Seq[ReportError] = Seq()
  var reportStats: Option[GenerateReportsStats] = None

  import context.dispatcher
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val receive: Receive = {
    case request: GenerateReports[_] =>
      origSender = Some(sender)
      reportSystem ! request
      bufferedChunkingActor ! InitiateChunkedUpload(TemporaryData.bucket, Some(fileName), FileStoreFileMetadata(fileName, TemporaryData))
      context become waitingForResult
  }

  val waitingForResult: Receive = {
    case SingleReportResult(_, stats, Left(error)) =>
      errors = errors :+ error
      notifyProgress(stats)

    case SingleReportResult(id, stats, Right(result: ReportResultWithDocument)) =>
      bufferedChunkingActor ! result
      notifyProgress(stats)

    case result: GenerateReportsStats if result.numberOfReportsInProgress == 0 =>
      reportStats = Some(result)
      bufferedChunkingActor ! CompleteChunkedTransfer
      context become waitingForResultPayload

    case stats: GenerateReportsStats =>
      notifyProgress(stats)

    case FileStoreError(error) =>
      log.warning(s"Error during buffered chunking file:$error")
      errors = errors :+ ReportError(None, s"Bei der Verarbeitung der Reportdokumente ist ein Fehler aufgetreten: $error")
  }

  val waitingForResultPayload: Receive = {
    case payload: FileStoreResultPayload =>
      jobFinished(reportStats.get, Some(payload))
      self ! PoisonPill

    case FileStoreError(error) =>
      log.error(s"Got error in waitingForResultPayload")
      jobFinished(reportStats.get, None)
      self ! PoisonPill
  }
}