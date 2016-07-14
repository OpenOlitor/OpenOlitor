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

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import com.typesafe.config.Config
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.LazyLogging

trait FileStoreComponent {
  val fileStore: FileStore
}

class DefaultFileStoreComponent(mandant: String, sysConfig: SystemConfig, system: ActorSystem) extends FileStoreComponent with LazyLogging {

  override lazy val fileStore = new S3FileStore(mandant, sysConfig.mandantConfiguration, system)

  fileStore.createBuckets map {
    _.fold(
      error => logger.error(s"Error creating buckets for $mandant: ${error.message}"),
      success => logger.debug(s"Created file store buckets for $mandant")
    )
  }
}
