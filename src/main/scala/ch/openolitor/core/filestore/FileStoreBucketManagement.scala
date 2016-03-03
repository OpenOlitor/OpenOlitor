package ch.openolitor.core.filestore

import scala.concurrent.Future
import ch.openolitor.core.Macros.SealedEnumeration

sealed trait FileStoreBucket
case object TemplatesBucket extends FileStoreBucket
case object RechnungenBucket extends FileStoreBucket
case object GenericBucket extends FileStoreBucket

object FileStoreBucket {
  val AllFileStoreBuckets = SealedEnumeration.values[FileStoreBucket]
}

trait FileStoreBucketManagement {
  def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]]
}

class S3FileStoreBucketManagement extends FileStoreBucketManagement {
  override def createBuckets: Future[Either[FileStoreError, FileStoreSuccess]] = {
    FileStoreBucket.AllFileStoreBuckets map {
      ???
    }
    ???
  }
}