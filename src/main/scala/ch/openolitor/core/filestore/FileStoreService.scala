package ch.openolitor.core.filestore

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import com.typesafe.config.Config
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.LazyLogging

trait FileStoreComponent {
  val fileStore: FileStore
}

trait DefaultFileStoreComponent extends FileStoreComponent with LazyLogging {
  val mandant: String
  val config: Config
  val system: ActorSystem

  override val fileStore = new S3FileStore(mandant, config, system)

  fileStore.createBuckets map {
    _.fold(
      error => logger.error(s"Error creating buckets for $mandant: ${error.message}"),
      success => logger.debug(s"Created file store buckets for $mandant")
    )
  }
}
