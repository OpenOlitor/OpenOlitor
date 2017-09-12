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

import scala.concurrent.Future
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration
import com.amazonaws.AmazonClientException
import scala.collection.JavaConversions._

trait FileStoreBucketLifeCycleConfiguration {
  def client: AmazonS3Client

  def bucketName(bucket: FileStoreBucket): String

  def configureLifeCycle(bucket: FileStoreBucket): Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        bucket match {
          case TemporaryDataBucket =>
            updateLifeCycle(bucket, new BucketLifecycleConfiguration.Rule().withExpirationInDays(7))

          case _ =>
          // nothing to configure
        }

        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not update the lifecycle of this bucket. $e"))
      }
    }
  }

  private def updateLifeCycle(bucket: FileStoreBucket, rules: BucketLifecycleConfiguration.Rule*) = {
    val configuration = new BucketLifecycleConfiguration().withRules(rules)
    client.setBucketLifecycleConfiguration(bucketName(bucket), configuration)
  }
}
