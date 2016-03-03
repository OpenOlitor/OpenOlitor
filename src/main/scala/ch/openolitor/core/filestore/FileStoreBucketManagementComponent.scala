package ch.openolitor.core.filestore

trait FileStoreBucketManagementComponent {
  val fileStoreBucketManagement: FileStoreBucketManagement
}

trait DefaultFileStoreBucketManagementComponent extends FileStoreBucketManagementComponent {
  override val fileStoreBucketManagement = new S3FileStoreBucketManagement
}