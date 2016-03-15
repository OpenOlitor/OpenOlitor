package ch.openolitor.core.filestore

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import com.typesafe.config.Config

object FileStoreService {
  def apply(mandant: String, config: Config, system: ActorSystem): FileStoreService = new DefaultFileStoreService(mandant, config, system)
}

trait FileStoreService {
  val fileStore: FileStore
}

class DefaultFileStoreService(mandant: String, config: Config, system: ActorSystem) extends FileStoreService {
  override val fileStore = new S3FileStore(mandant, config, system)
}

