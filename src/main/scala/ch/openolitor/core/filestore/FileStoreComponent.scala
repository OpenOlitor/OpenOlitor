package ch.openolitor.core.filestore

trait TemplateFileStoreComponent {
  val templateFileStore: FileStore
}

trait DefaultTemplateFileStoreComponent extends TemplateFileStoreComponent {
  override val templateFileStore = new S3FileStore(TemplatesBucket)
}

trait RechnungenFileStoreComponent {
  val rechnungenFileStore: FileStore
}
trait DefaultRechnungenFileStoreComponent extends RechnungenFileStoreComponent {
  override val rechnungenFileStore = new S3FileStore(RechnungenBucket)
}

trait GenericFileStoreComponent {
  val genericFileStore: FileStore
}

trait DefaultGenericFileStoreComponent extends GenericFileStoreComponent {
  override val genericFileStore = new S3FileStore(GenericBucket)
}

trait FileStoreComponent extends TemplateFileStoreComponent with RechnungenFileStoreComponent with GenericFileStoreComponent

trait DefaultFileStoreComponent extends DefaultTemplateFileStoreComponent with DefaultRechnungenFileStoreComponent with DefaultGenericFileStoreComponent