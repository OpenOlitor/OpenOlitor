package ch.openolitor.stammdaten.eventsourcing

import stamina._
import stamina.json._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol

object StammdatenEventStoreSerializer extends StammdatenJsonProtocol with EntityStoreJsonProtocol{
  
  //declare missing json formats
  implicit val insertDepotFormat = jsonFormat3(EntityInsertedEvent[DepotModify])
  
  //V1 persisters
  val insertDepotPersister = persister[EntityInsertedEvent[DepotModify]]("insert-depot")
}