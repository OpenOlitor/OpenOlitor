package ch.openolitor.util

import ch.openolitor.stammdaten.models.CustomKundentypCreate
import ch.openolitor.stammdaten.models.KundentypId
import spray.json._
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.core.domain.EntityStore._
import java.util.UUID
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.models.UserId
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer

object TestKundetypFormat extends App with StammdatenJsonProtocol  with StammdatenEventStoreSerializer{
  val typ = CustomKundentypCreate(KundentypId("dasad"), None)
  val event = EntityInsertedEvent(EventMetadata(UserId(UUID.randomUUID), 1, System.currentTimeMillis, 1, "test"), 
      UUID.randomUUID, typ)
  println(s"toJson:${typ.toJson}")
  println(s"toJson2:${event.toJson}")
  
  println(s"Persist:${insertCustomKundetypPersister.persist(event)}")
}