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

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import stamina._
import stamina.json._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models.CustomKundentyp
import ch.openolitor.stammdaten.models.CustomKundentypCreate
import ch.openolitor.core.eventsourcing.events._
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer

class EventStoreSerializer extends StaminaAkkaSerializer(EventStoreSerializer.eventStorePersisters)
    with LazyLogging
    with StammdatenEventStoreSerializer
    with BuchhaltungEventStoreSerializer {

  override def toBinary(obj: AnyRef): Array[Byte] = {
    logger.debug(s"EventStoreSerializer: toBinary: $obj")
    try {
      super.toBinary(obj)
    } catch {
      case e: Exception =>
        logger.error(s"Can't persist $obj", e)
        stammdatenPersisters.map { persister =>
          if (persister.canPersist(obj)) {
            logger.warn(s"Found persister:${persister.key}")
          }
        }
        throw e
    }
  }
}

object EventStoreSerializer extends EntityStoreJsonProtocol
    with StammdatenEventStoreSerializer
    with BuchhaltungEventStoreSerializer {

  val entityPersisters = Persisters(stammdatenPersisters ++ buchhaltungPersisters)

  val entityStoreInitializedPersister = persister[EntityStoreInitialized]("entity-store-initialized")
  val entityInsertEventPersister = new EntityInsertEventPersister[V1](entityPersisters)
  val entityUpdatedEventPersister = new EntityUpdatedEventPersister[V1](entityPersisters)
  val entityDeletedEventPersister = new EntityDeletedEventPersister[V1](entityPersisters)

  val eventStorePersisters = List(entityStoreInitializedPersister, entityInsertEventPersister, entityUpdatedEventPersister, entityDeletedEventPersister) ++ stammdatenPersisters ++ buchhaltungPersisters
}