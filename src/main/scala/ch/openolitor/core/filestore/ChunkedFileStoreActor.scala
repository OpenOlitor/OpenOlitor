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
package ch.openolitor.core.filestore

import akka.actor._
import java.util.UUID
import ch.openolitor.util.ByteBufferBackedInputStream
import scala.concurrent.ExecutionContext
import java.io.ByteArrayInputStream
import scala.concurrent.Future
import java.io.File
import java.io.InputStream
import java.io.FileInputStream
import scala.concurrent.Await
import scala.concurrent.duration._

object ChunkedFileStoreActor {
  def props(fileStore: FileStore): Props = Props(classOf[ChunkedFileStoreActor], fileStore)

  case class InitiateChunkedUpload(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata)
  case class UploadChunk(metadata: FileStoreChunkedUploadMetaData, file: InputStream, partSize: Int, partNumber: Int)
  case class CompleteChunkedUpload(metadata: FileStoreChunkedUploadMetaData)
  case class AbortChunkedUpload(metadata: FileStoreChunkedUploadMetaData)
  case object ChunkedUploadCompleted
  case object ChunkedUploadAborted
}

class ChunkedFileStoreActor(fileStore: FileStore) extends Actor with ActorLogging {
  import ChunkedFileStoreActor._

  implicit val ctx: ExecutionContext = context.system.dispatcher

  var etags: List[FileStoreChunkedUploadPartEtag] = Nil

  val receive: Receive = {
    case InitiateChunkedUpload(bucket, id, metadata) =>
      val rec = sender
      val name = id.getOrElse(UUID.randomUUID.toString)

      fileStore.initiateChunkedUpload(bucket, Some(name), metadata) map {
        case Left(e) =>
          rec ! e
          self ! PoisonPill
        case Right(result) =>
          rec ! result
          context become initialized
      }
  }

  val initialized: Receive = {
    case UploadChunk(metadata, inputStream, partSize, partNumber) =>
      val rec = sender

      fileStore.uploadChunk(metadata, inputStream, partSize, partNumber) map {
        case Left(e) =>
          rec ! e
        case Right(result) =>
          etags = etags :+ result
          rec ! result
      }

    case CompleteChunkedUpload(metadata) =>
      val rec = sender

      fileStore.completeChunkedUpload(metadata, etags sortBy (_.partNumber)) map {
        case Left(e) =>
          rec ! e
        case Right(result) =>
          rec ! ChunkedUploadCompleted
      }

      self ! PoisonPill

    case AbortChunkedUpload(metadata) =>
      val rec = sender

      fileStore.abortChunkedUpload(metadata) map {
        case Left(e) =>
          rec ! e
        case Right(result) =>
          rec ! ChunkedUploadAborted
      }

      self ! PoisonPill

  }
}