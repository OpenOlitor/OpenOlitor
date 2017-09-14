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
package ch.openolitor.core.filestore.batch

import akka.actor.Actor
import akka.actor.ActorLogging
import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import akka.actor.Props
import ch.openolitor.core.batch.BatchJobs._
import ch.openolitor.core.batch.BaseBatchJobsSupervisor
import ch.openolitor.core.filestore.batch.housekeeping.TemporaryDataBucketCleanupBatchJob
import akka.actor.ActorRef
import ch.openolitor.core.filestore.FileStore

object FileStoreBatchJobs {
  def props(sysConfig: SystemConfig, system: ActorSystem, fileStore: FileStore): Props = Props(classOf[DefaultFileStoreBatchJobs], sysConfig, system, fileStore)
}

class FileStoreBatchJobs(val sysConfig: SystemConfig, val system: ActorSystem, val fileStore: FileStore) extends BaseBatchJobsSupervisor {
  override lazy val batchJobs = Set(
    context.actorOf(TemporaryDataBucketCleanupBatchJob.props(sysConfig, system, fileStore))
  )
}

class DefaultFileStoreBatchJobs(override val sysConfig: SystemConfig, override val system: ActorSystem, override val fileStore: FileStore) extends FileStoreBatchJobs(sysConfig, system, fileStore)
