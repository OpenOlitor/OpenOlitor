package ch.openolitor.core.eventsourcing

import spray.json._
import stamina._
import stamina.json._
import org.specs2.mutable.Specification
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.models.UserId
import java.util.UUID
import ch.openolitor.core.eventsourcing.events._
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.BaseJsonProtocol

case class TestId(id:UUID = UUID.randomUUID) extends BaseId
case class TestEntity(msg:String, number:Int)

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
    val persister = new EntityInsertEventPersister[V1](persisters)
    
    "persist and unpersist correctly" in {
      val meta = EventMetadata(UserId(UUID.randomUUID), 1, System.currentTimeMillis, 1L, "test")
      val event = EntityInsertedEvent(meta, UUID.randomUUID, TestEntity("test", 1234))
      
      persister.unpersist(persister.persist(event.asInstanceOf[EntityInsertedEvent[AnyRef]])) === event
    }
  }
  
  "EntityUpdatedEvent" should {
    val persister = new EntityUpdatedEventPersister[V1](persisters)
    
    "persist and unpersist correctly" in {
      val meta = EventMetadata(UserId(UUID.randomUUID), 1, System.currentTimeMillis, 1L, "test")
      val event = EntityUpdatedEvent(meta, TestId(), TestEntity("test", 1234))
      
      persister.unpersist(persister.persist(event.asInstanceOf[EntityUpdatedEvent[BaseId, AnyRef]])) === event
    }
  }
  
  "EntityDeletedEvent" should {
    val persister = new EntityDeletedEventPersister[V1](persisters)
    
    "persist and unpersist correctly" in {
      val meta = EventMetadata(UserId(UUID.randomUUID), 1, System.currentTimeMillis, 1L, "test")
      val event = EntityDeletedEvent(meta, TestId())
      
      persister.unpersist(persister.persist(event.asInstanceOf[EntityDeletedEvent[BaseId]])) === event
    }
  }
}