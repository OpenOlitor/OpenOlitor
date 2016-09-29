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
package ch.openolitor.Buchhaltung

import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.buchhaltung.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.PersonId
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler._
import ch.openolitor.buchhaltung.repositories._
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ch.openolitor.util.ConfigUtil._
import scalikejdbc.DBSession
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.repositories.SqlBinder

object BuchhaltungGeneratedEventsService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungGeneratedEventsService = new DefaultBuchhaltungGeneratedEventsService(sysConfig, system)
}

class DefaultBuchhaltungGeneratedEventsService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungGeneratedEventsService(sysConfig) with DefaultBuchhaltungWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten generierter Events (z.B. datumsabhängige Events)
 */
class BuchhaltungGeneratedEventsService(override val sysConfig: SystemConfig) extends EventService[PersistentGeneratedEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings with BuchhaltungEventStoreSerializer {
  self: BuchhaltungWriteRepositoryComponent =>

  val handle: Handle = {
    case _ =>
    // nothing to handle
  }

}
