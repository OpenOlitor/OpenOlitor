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
import java.io.InputStream
import java.util.UUID
import akka.actor.ActorSystem
import com.typesafe.config.Config
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import ch.openolitor.core.MandantConfiguration
import com.typesafe.scalalogging.LazyLogging
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.ListBucketsRequest
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.auth.AWS3Signer
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.S3ClientOptions
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.models.BaseStringId

case class FileStoreError(message: String)
case class FileStoreSuccess()

case class FileStoreFileMetadata(name: String, fileType: FileType)
case class FileStoreFile(metaData: FileStoreFileMetadata, file: InputStream)
case class FileStoreFileId(id: String) extends BaseStringId
case class FileStoreFileReference(fileType: FileType, id: FileStoreFileId) extends JSONSerializable

trait FileStore {
  val mandant: String

  def getFileIds(bucket: FileStoreBucket): Future[Either[FileStoreError, List[FileStoreFileId]]]

  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]]

  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]]

  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]]

  def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]]

  def bucketName(bucket: FileStoreBucket) = {
    s"${mandant.toLowerCase}-${bucket.toString.toLowerCase}"
  }
}

class S3FileStore(override val mandant: String, mandantConfiguration: MandantConfiguration, actorSystem: ActorSystem) extends FileStore with LazyLogging {
  val opts = new ClientConfiguration
  opts.setSignerOverride("S3SignerType")

  lazy val client = {
    val c = new AmazonS3Client(new BasicAWSCredentials(mandantConfiguration.config.getString("s3.aws-access-key-id"), mandantConfiguration.config.getString("s3.aws-secret-acccess-key")), opts)
    c.setEndpoint(mandantConfiguration.config.getString("s3.aws-endpoint"))
    c.setS3ClientOptions(S3ClientOptions.builder.setPathStyleAccess(true).build())
    c
  }

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

    Future.successful {
      try {
        Right(client.listObjects(listRequest).getObjectSummaries.toList.map(s => FileStoreFileId(s.getKey)))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError("Could not get file ids."))
      }
    }
  }

  def getFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreFile]] = {
    Future.successful {
      try {
        val result = client.getObject(new GetObjectRequest(bucketName(bucket), id))
        Right(FileStoreFile(transform(result.getObjectMetadata.getUserMetadata.toMap), result.getObjectContent))
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not get file. ${e}"))
      }
    }
  }

  def putFile(bucket: FileStoreBucket, id: Option[String], metadata: FileStoreFileMetadata, file: InputStream): Future[Either[FileStoreError, FileStoreFileMetadata]] = {
    Future.successful {
      try {
        val result = client.putObject(new PutObjectRequest(bucketName(bucket), id.getOrElse(generateId), file, transform(metadata)))
        Right(metadata)
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not put file. ${e}"))
      }
    }
  }

  def deleteFile(bucket: FileStoreBucket, id: String): Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        val result = new DeleteObjectRequest(bucketName(bucket), id)
        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError("Could not delete file."))
      }
    }
  }

  override def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]] = {
    Future.successful {
      try {
        val result = FileStoreBucket.AllFileStoreBuckets map { b =>
          client.createBucket(new CreateBucketRequest(bucketName(b)))
        }
        Right(FileStoreSuccess())
      } catch {
        case e: AmazonClientException =>
          Left(FileStoreError(s"Could not create buckets. $e"))
      }
    }
  }
}