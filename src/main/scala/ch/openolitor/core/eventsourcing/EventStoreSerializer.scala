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

class EventStoreSerializer extends StaminaAkkaSerializer(EventStoreSerializer.allPersisters) with LazyLogging {
  val persisters = Persisters(EventStoreSerializer.allPersisters)
  
  override def toBinary(obj: AnyRef): Array[Byte] = {
    logger.debug(s"EventStoreSerielizer: toBinary: $obj")
    try {
      super.toBinary(obj)
    }          
    catch {
      case e:Exception => 
        logger.error(s"Can't persist $obj", e)
        null
    }
  }
}

object EventStoreSerializer extends EntityStoreJsonProtocol with StammdatenEventStoreSerializer { 
  val entityStoreInitializedPersister = persister[EntityStoreInitialized]("entity-store-initialized")
  
  val eventStorePersisters = List(entityStoreInitializedPersister)
  
  val allPersisters = eventStorePersisters ++ stammdatenPersisters
}