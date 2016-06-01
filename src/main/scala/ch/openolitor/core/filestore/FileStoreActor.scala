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
import akka.util.ByteString
import java.util.UUID
import ch.openolitor.util.ByteBufferBackedInputStream
import scala.concurrent.ExecutionContext

object FileStoreActor {
  def props(fileStore: FileStore): Props = Props(classOf[FileStoreActor], fileStore)

  case class StoreFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: ByteString)
}

class FileStoreActor(fileStore: FileStore) extends Actor with ActorLogging {
  import FileStoreActor._

  implicit val ctx: ExecutionContext = context.system.dispatcher

  val receive: Receive = {
    case StoreFile(bucket, id, metadata, file) =>
      val rec = sender
      storeFile(bucket, id, metadata, file) map {
        case Left(e) => rec ! e
        case Right(result) => rec ! result
      }
  }

  def storeFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: ByteString) = {
    val name = id.getOrElse(UUID.randomUUID.toString)
    val is = new ByteBufferBackedInputStream(file.asByteBuffer)
    fileStore.putFile(bucket, Some(name), metadata, is)
  }
}