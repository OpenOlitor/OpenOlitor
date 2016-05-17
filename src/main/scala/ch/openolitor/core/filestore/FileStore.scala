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
import com.amazonaws.services.s3.model.GetObjectRequest
import java.io.InputStream
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.PutObjectRequest
import java.util.UUID
import com.amazonaws.services.s3.model.ObjectMetadata
import com.sclasen.spray.aws.s3.S3Client
import akka.actor.ActorSystem
import com.sclasen.spray.aws.s3.S3ClientProps
import com.typesafe.config.Config
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.services.s3.model.ListBucketsRequest
import ch.openolitor.core.MandantConfiguration

case class FileStoreError(message: String)
case class FileStoreSuccess()

case class FileStoreFileMetadata(name: String, fileType: FileType)
case class FileStoreFile(metaData: FileStoreFileMetadata, file: InputStream)
case class FileStoreFileId(id: String)

trait FileStore {
  val mandant: String

  def getFileIds(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileId]]]

  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]]

  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]]

  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]]

  def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]]

  def bucketName(bucket: FileStoreBucket) = {
    s"${mandant}_${bucket.toString}"
  }
}

class S3FileStore(override val mandant: String, mandantConfiguration: MandantConfiguration, actorSystem: ActorSystem) extends FileStore with LazyLogging {
  lazy val createSystem = ActorSystem(mandant, mandantConfiguration.config)

  def props = S3ClientProps(
    mandantConfiguration.config.getString("s3.aws-access-key-id"),
    mandantConfiguration.config.getString("s3.aws-secret-acccess-key"),
    Timeout(30 seconds),
    createSystem,
    createSystem,
    mandantConfiguration.config.getString("s3.aws-endpoint")
  )

  val client = new S3Client(props)

  def generateId = UUID.randomUUID.toString

  def transform(metadata: Map[String, String]): FileStoreFileMetadata = {
    if (metadata.isEmpty) logger.warn("There was no metadata stored with this file.")
    val fileType = FileType(metadata.get("fileType").getOrElse("").toLowerCase)
    FileStoreFileMetadata(metadata.get("name").getOrElse(""), fileType)
  }

  def transform(metadata: FileStoreFileMetadata): ObjectMetadata = {
    val result = new ObjectMetadata()
    result.addUserMetadata("name", metadata.name)
    result.addUserMetadata("fileType", metadata.fileType.getClass.getSimpleName)
    result
  }

  def getFileIds(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileId]]] = {
    val listRequest = new ListObjectsRequest()
    listRequest.setBucketName(bucketName(bucket))
    client.listObjects(listRequest) map {
      _.fold(
        e => Left(FileStoreError("Could not get file ids.")),
        ol => Right(ol.getObjectSummaries.toList.map(s => FileStoreFileId(s.getKey)))
      )
    }
  }

  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]] = {
    client.getObject(new GetObjectRequest(bucketName(bucket), id)) map {
      _.fold(
        e => Left(FileStoreError(s"Could not get file. ${e}")),
        o => Right(FileStoreFile(transform(o.getObjectMetadata.getUserMetadata.toMap), o.getObjectContent))
      )
    }
  }

  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]] = {
    client.putObject(new PutObjectRequest(bucketName(bucket), id.getOrElse(generateId), file, transform(metadata))) map {
      _.fold(
        e => Left(FileStoreError(s"Could not put file. ${e}")),
        o => Right(metadata)
      )
    }
  }

  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]] = {
    client.deleteObject(new DeleteObjectRequest(bucketName(bucket), id)) map {
      _.fold(
        e => Left(FileStoreError("Could not delete file.")),
        o => Right(FileStoreSuccess())
      )
    }
  }

  override def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]] = {
    val res = FileStoreBucket.AllFileStoreBuckets map { b =>
      client.createBucket(new CreateBucketRequest(bucketName(b))) map {
        _.fold(e => Left(e), _ => Right(true))
      }
    }

    // fail on first error or else succeed
    Future.sequence(res) map { seq =>
      seq.collectFirst {
        case Left(ex) => Left(FileStoreError(s"Could not create all buckets $ex"))
      } getOrElse {
        Right(FileStoreSuccess())
      }
    }
  }
}