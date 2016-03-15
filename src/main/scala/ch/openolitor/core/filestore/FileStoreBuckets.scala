package ch.openolitor.core.filestore

import scala.concurrent.Future
import ch.openolitor.core.Macros.SealedEnumeration
import com.amazonaws.services.s3.model.CreateBucketRequest

sealed trait FileStoreBucket
case object VorlagenBucket extends FileStoreBucket
case object GeneriertBucket extends FileStoreBucket
case object StammdatenBucket extends FileStoreBucket
case object EsrBucket extends FileStoreBucket

object FileStoreBucket {
  val AllFileStoreBuckets = SealedEnumeration.values[FileStoreBucket]
}