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

import spray.json._
import stamina._
import stamina.json._
import org.specs2.mutable.Specification
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.eventsourcing.events._
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.BaseJsonProtocol
import org.joda.time.DateTime

case class TestId(id: Long = (Math.random * 10000).toLong) extends BaseId
case class TestEntity(msg: String, number: Int)

object TestId extends BaseJsonProtocol {
  implicit val testIdFormat = jsonFormat1(TestId.apply)
  val testIdPersister = persister[TestId]("test-id")
}

object TestEntity extends DefaultJsonProtocol {
  implicit val testEntityFormat = jsonFormat2(TestEntity.apply)

  val testEntityPersister = persister[TestEntity]("test-entity")
}

class EnvelopePersistersSpec extends Specification {
  import TestEntity._
  import TestId._
  val persisters = Persisters(List(testEntityPersister, testIdPersister))

  "EntityInsertEvent" should {
    val persister = new EntityInsertEventPersister(persisters)

    "persist and unpersist correctly" in {
      val meta = EventMetadata(PersonId(23), 1, DateTime.now, 1L, 1L, "test")
      val event = EntityInsertedEvent(meta, TestId(), TestEntity("test", 1234))

      persister.unpersist(persister.persist(event.asInstanceOf[EntityInsertedEvent[BaseId, AnyRef]])) === event
    }
  }

  "EntityUpdatedEvent" should {
    val persister = new EntityUpdatedEventPersister(persisters)

    "persist and unpersist correctly" in {
      val meta = EventMetadata(PersonId(24), 1, DateTime.now, 1L, 1L, "test")
      val event = EntityUpdatedEvent(meta, TestId(), TestEntity("test", 1234))

      persister.unpersist(persister.persist(event.asInstanceOf[EntityUpdatedEvent[BaseId, AnyRef]])) === event
    }
  }

  "EntityDeletedEvent" should {
    val persister = new EntityDeletedEventPersister(persisters)

    "persist and unpersist correctly" in {
      val meta = EventMetadata(PersonId(25), 1, DateTime.now, 1L, 1L, "test")
      val event = EntityDeletedEvent(meta, TestId())

      persister.unpersist(persister.persist(event.asInstanceOf[EntityDeletedEvent[BaseId]])) === event
    }
  }
}