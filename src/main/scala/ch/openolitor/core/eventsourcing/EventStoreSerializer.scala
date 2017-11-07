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
package ch.openolitor.core.eventsourcing

import stamina._
import stamina.json._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.eventsourcing.events._
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import ch.openolitor.reports.eventsourcing.ReportsEventStoreSerializer

class EventStoreSerializer extends StaminaAkkaSerializer(EventStoreSerializer.eventStorePersisters)
    with LazyLogging {

  override def toBinary(obj: AnyRef): Array[Byte] = {
    logger.debug(s"EventStoreSerializer: toBinary: $obj")
    try {
      super.toBinary(obj)
    } catch {
      case e: Exception =>
        logger.error(s"Can't persist $obj", e)
        throw e
    }
  }
}

object EventStoreSerializer extends EntityStoreJsonProtocol
    with StammdatenEventStoreSerializer
    with BuchhaltungEventStoreSerializer
    with ReportsEventStoreSerializer
    with CoreEventStoreSerializer
    with SystemEventSerializer {

  // entity store serialization
  val entityPersisters = Persisters(corePersisters ++ stammdatenPersisters ++ buchhaltungPersisters ++ reportsPersisters)
  val entityStoreInitializedPersister = persister[EntityStoreInitialized, V2]("entity-store-initialized", V1toV2metaDataMigration)
  val entityInsertEventPersister = new EntityInsertEventPersister(entityPersisters)
  val entityUpdatedEventPersister = new EntityUpdatedEventPersister(entityPersisters)
  val entityDeletedEventPersister = new EntityDeletedEventPersister(entityPersisters)

  // system event serialization
  val eventPersisters = Persisters(systemEventPersisters)
  val systemEventPersister = new SystemEventPersister(eventPersisters)

  // mail event serialization
  val sendMailEventPersister = new SendMailEventPersister(entityPersisters ++ eventPersisters)
  val mailSentEventPersister = new MailSentEventPersister(entityPersisters ++ eventPersisters)
  val sendMailFailedEventPersister = new SendMailFailedEventPersister(entityPersisters ++ eventPersisters)

  val eventStorePersisters = List(
    entityStoreInitializedPersister,
    entityInsertEventPersister,
    entityUpdatedEventPersister,
    entityDeletedEventPersister,
    systemEventPersister,
    sendMailEventPersister,
    mailSentEventPersister,
    sendMailFailedEventPersister
  ) ++
    corePersisters ++
    stammdatenPersisters ++
    buchhaltungPersisters ++
    reportsPersisters ++
    systemEventPersisters

  val allPersisters = Persisters(eventStorePersisters)
}
