package ch.openolitor.core.filestore

import scala.concurrent.Future
import com.amazonaws.services.s3.model.CreateBucketRequest

sealed trait FileStoreBucket
case object VorlagenBucket extends FileStoreBucket
case object GeneriertBucket extends FileStoreBucket
case object StammdatenBucket extends FileStoreBucket
case object EsrBucket extends FileStoreBucket

object FileStoreBucket {
  val AllFileStoreBuckets = List(VorlagenBucket, GeneriertBucket, StammdatenBucket, EsrBucket)
}