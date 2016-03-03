package ch.openolitor.core.filestore

import scala.concurrent.Future

case class FileStoreError(message: String)
case class FileStoreSuccess()

case class FileStoreFileMetadata()
case class FileStoreFile()

trait FileStore {
  val bucket: FileStoreBucket

  def getFile: Future[Either[FileStoreError, FileStoreFile]]

  def putFile: Future[Either[FileStoreError, FileStoreFileMetadata]]

  def deleteFile: Future[Either[FileStoreError, FileStoreSuccess]]
}

class S3FileStore(override val bucket: FileStoreBucket) extends FileStore {
  def getFile: Future[Either[FileStoreError, FileStoreFile]] = {
    ???
  }

  def putFile: Future[Either[FileStoreError, FileStoreFileMetadata]] = {
    ???
  }

  def deleteFile: Future[Either[FileStoreError, FileStoreSuccess]] = {
    ???
  }
}