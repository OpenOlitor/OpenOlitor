package ch.openolitor.core.filestore

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import com.typesafe.config.Config

trait FileStoreComponent {
  val fileStore: FileStore
}

trait DefaultFileStoreComponent extends FileStoreComponent {
  val mandant: String
  val config: Config
  val system: ActorSystem

  override val fileStore = new S3FileStore(mandant, config, system)
}
