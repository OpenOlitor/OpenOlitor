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
import ch.openolitor.core.filestore.ChunkedFileStoreActor._
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
import scala.collection.mutable.ArrayBuffer
import java.io.File

object BufferedChunkingActor {
  case object CompleteChunkedTransfer

  val DefaultChunkSize = 5 * 1024 * 1024

  /**
   * @param chunkSize has to be >= 5MB
   */
  def props(fileStore: FileStore, fileName: String, chunkSize: Int = DefaultChunkSize): Props = Props(classOf[BufferedChunkingActor], fileStore, fileName, chunkSize)
}

/**
 * Buffer and chunk messages according to buffer size.
 */
class BufferedChunkingActor(fileStore: FileStore, fileName: String, chunkSize: Int) extends OutputStream with Actor with ActorLogging with DateFormats {
  import BufferedChunkingActor._

  val chunkedFileStoreActor = context.actorOf(ChunkedFileStoreActor.props(fileStore), "chunked-file-store-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None
  var errors: Seq[ReportError] = Seq()

  var metadata: Option[FileStoreChunkedUploadMetaData] = None

  val zipBuilder: ZipBuilder = new ZipBuilder(this)
  var lastPart: Option[Int] = None
  var partNumber = 0

  val bytes = ArrayBuffer[Byte]()

  // overriding write to buffer all the bytes until chunkSize is reached
  override def write(b: Int): Unit = {
    bytes += b.toByte

    if (bytes.length == chunkSize) {
      sendPart()
    }
  }

  // overriding close to know the last part
  override def close(): Unit = {
    lastPart = Some(partNumber + 1)
    sendPart()
  }

  val receive: Receive = {
    case initiate: InitiateChunkedUpload =>
      origSender = Some(sender)
      chunkedFileStoreActor ! initiate
      context become waitingForChunkedUploadInitilization
  }

  val waitingForChunkedUploadInitilization: Receive = {
    case result: FileStoreChunkedUploadMetaData =>
      metadata = Some(result)
      context become waitingForChunks

    case FileStoreError(error) =>
      origSender map (_ ! error)
      self ! PoisonPill
  }

  /**
   * Receiving ReportResults, adding the contents to the zip and
   */
  val waitingForChunks: Receive = {
    // adding the entry to the zip, sending of parts will happen in write
    case result: ReportResultWithDocument =>
      zipBuilder.addZipEntry(result.name, result.document)

    case result: FileStoreChunkedUploadPartEtag if (lastPart.isDefined && lastPart.get == result.partNumber) =>
      log.debug(s"received last part with partNumber $partNumber")
      chunkedFileStoreActor ! CompleteChunkedUpload(metadata.get)
      context become waitingForChunkedUploadCompletion

    case result: FileStoreChunkedUploadPartEtag =>
      log.debug("received part")
    // TODO update progress based on chunks

    case FileStoreError(error) =>
      origSender map (_ ! error)

    case CompleteChunkedTransfer =>
      zipBuilder.close()
  }

  val waitingForChunkedUploadCompletion: Receive = {
    case ChunkedUploadCompleted =>
      origSender map (_ ! FileStoreResultPayload(Seq(FileStoreFileReference(metadata.get.metadata.fileType, FileStoreFileId(metadata.get.key)))))
      self ! PoisonPill

    case error: FileStoreError =>
      origSender map (_ ! error)
      chunkedFileStoreActor ! AbortChunkedUpload(metadata.get)
      self ! PoisonPill
  }

  /**
   * Send this part to the file store and clear the buffer
   */
  private def sendPart() = {
    partNumber += 1
    chunkedFileStoreActor ! UploadChunk(metadata.get, new ByteArrayInputStream(bytes.toArray, 0, bytes.length), bytes.length, partNumber)
    bytes.clear()
  }
}