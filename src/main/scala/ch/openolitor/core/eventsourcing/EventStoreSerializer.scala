package ch.openolitor.core.eventsourcing

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import stamina._
import stamina.json._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.domain.EntityStore._

class EventStoreSerializer extends StaminaAkkaSerializer(EventStoreSerializer.entityStoreInitializedPersister)

object EventStoreSerializer extends EntityStoreJsonProtocol { 
  val entityStoreInitializedPersister = persister[EntityStoreInitialized]("entity-store-initialized")
  
  val eventStorePersisters = List(entityStoreInitializedPersister)
}