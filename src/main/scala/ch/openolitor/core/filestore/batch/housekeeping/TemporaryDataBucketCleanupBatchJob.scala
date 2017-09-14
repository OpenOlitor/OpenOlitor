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
package ch.openolitor.core.filestore.batch.housekeeping

import akka.actor.ActorSystem
import akka.actor.Props
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.batch.BaseBatchJob
import ch.openolitor.core.batch.BatchJobs._
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import ch.openolitor.core.filestore.FileStore
import ch.openolitor.core.filestore.TemporaryDataBucket
import ch.openolitor.util.DateTimeUtil._
import com.github.nscala_time.time.Imports._
import com.github.nscala_time.time.DurationBuilder
import scala.concurrent.duration.{ Duration => ScalaDuration, FiniteDuration }

object TemporaryDataBucketCleanupBatchJob {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem, fileStore: FileStore): Props = Props(classOf[TemporaryDataBucketCleanupBatchJob], sysConfig, system, fileStore)
}

class TemporaryDataBucketCleanupBatchJob(val sysConfig: SystemConfig, val system: ActorSystem, val fileStore: FileStore) extends BaseBatchJob {
  import system.dispatcher

  val ExpiryTime: Duration = sysConfig.mandantConfiguration.config.getDuration("s3.temporarydata.expirytime")

  override def process(): Unit = {
    fileStore.getFileSummaries(TemporaryDataBucket) map {
      case Left(error) =>
        logger.error(s"Could not get the file summaries. $error")

      case Right(summaries) =>
        // temporary files shouldn't have been modified
        summaries filter (_.lastModified + ExpiryTime < DateTime.now) map (_.key) match {
          case Nil => // Nothing to delete

          case ids =>
            fileStore.deleteFiles(TemporaryDataBucket, ids) map {
              case Left(error) =>
                logger.error(s"Could not delete the files. $error")
              case Right(result) =>
                logger.debug(s"Successfully deleted temporary ${ids.size} of ${summaries.size} files.")
            }
        }
    }
  }

  protected def handleInitialization(): Unit = {
    // perform cleanup nightly
    batchJob = Some(context.system.scheduler.schedule(untilNextMidnight + (2 hours), 24 hours)(self ! StartBatchJob))
  }
}
