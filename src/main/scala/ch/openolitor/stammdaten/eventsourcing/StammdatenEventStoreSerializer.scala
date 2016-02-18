package ch.openolitor.stammdaten.eventsourcing

import stamina._
import stamina.json._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol

trait StammdatenEventStoreSerializer extends StammdatenJsonProtocol with EntityStoreJsonProtocol{
  
  //TODO: replace by macro
  
  //declare missing json formats
  implicit val insertDepotFormat = jsonFormat3(EntityInsertedEvent[DepotModify])
  implicit val updateDepotFormat = jsonFormat3(EntityUpdatedEvent[DepotId, DepotModify])
  implicit val deleteDepotFormat = jsonFormat2(EntityDeletedEvent[DepotId])
  
  implicit val insertAboFormat = jsonFormat3(EntityInsertedEvent[AboModify])
  implicit val updateAboFormat = jsonFormat3(EntityUpdatedEvent[AboId, AboModify])
  implicit val deleteAboFormat = jsonFormat2(EntityDeletedEvent[AboId])
  
  implicit val insertAbotypFormat = jsonFormat3(EntityInsertedEvent[AbotypModify])
  implicit val updateAbotypFormat = jsonFormat3(EntityUpdatedEvent[AbotypId, AbotypModify])
  implicit val deleteAbotypFormat = jsonFormat2(EntityDeletedEvent[AbotypId])
  
  implicit val insertKundeFormat = jsonFormat3(EntityInsertedEvent[KundeModify])
  implicit val updateKundeFormat = jsonFormat3(EntityUpdatedEvent[KundeId, KundeModify])
  implicit val deleteKundeFormat = jsonFormat2(EntityDeletedEvent[KundeId])
  
  implicit val insertVertriebsartDLFormat = jsonFormat3(EntityInsertedEvent[DepotlieferungAbotypModify])
  implicit val updateVertriebsartDLFormat = jsonFormat3(EntityUpdatedEvent[VertriebsartId, DepotlieferungAbotypModify])
  implicit val insertVertriebsartPLFormat = jsonFormat3(EntityInsertedEvent[PostlieferungAbotypModify])
  implicit val updateVertriebsartPLFormat = jsonFormat3(EntityUpdatedEvent[VertriebsartId, PostlieferungAbotypModify])
  implicit val insertVertriebsartHLFormat = jsonFormat3(EntityInsertedEvent[HeimlieferungAbotypModify])
  implicit val updateVertriebsartHLFormat = jsonFormat3(EntityUpdatedEvent[VertriebsartId, HeimlieferungAbotypModify])
  implicit val deleteVertriebsartFormat = jsonFormat2(EntityDeletedEvent[VertriebsartId])
  
  implicit val insertCustomKundetypFormat = jsonFormat3(EntityInsertedEvent[CustomKundentypCreate])
  implicit val updateCustomKundetypFormat = jsonFormat3(EntityUpdatedEvent[CustomKundentypId, CustomKundentypModify])
  implicit val deleteCustomKundetypFormat = jsonFormat2(EntityDeletedEvent[CustomKundentypId])
  
  implicit val insertPendenzFormat = jsonFormat3(EntityInsertedEvent[PendenzModify])
  implicit val updatePendenzFormat = jsonFormat3(EntityUpdatedEvent[PendenzId, PendenzModify])
  implicit val deletePendenzFormat = jsonFormat2(EntityDeletedEvent[PendenzId])
  
  implicit val insertLieferungFormat = jsonFormat3(EntityInsertedEvent[LieferungAbotypCreate])  
  implicit val deleteLieferungFormat = jsonFormat2(EntityDeletedEvent[LieferungId])
  
  //V1 persisters
  implicit val insertDepotPersister = persister[EntityInsertedEvent[DepotModify]]("insert-depot")
  implicit val updateDepotPersister = persister[EntityUpdatedEvent[DepotId, DepotModify]]("update-depot")
  implicit val deleteDepotPersister = persister[EntityDeletedEvent[DepotId]]("delete-depot")
  
  implicit val insertAboPersister = persister[EntityInsertedEvent[AboModify]]("insert-abo")
  implicit val updateAboPersister = persister[EntityUpdatedEvent[AboId, AboModify]]("update-abo")
  implicit val deleteAboPersister = persister[EntityDeletedEvent[AboId]]("delete-abo")
  
  implicit val insertAbotypPersister = persister[EntityInsertedEvent[AbotypModify]]("insert-abotyp")
  implicit val updateAbotypPersister = persister[EntityUpdatedEvent[AbotypId, AbotypModify]]("update-abotyp")
  implicit val deleteAbotypPersister = persister[EntityDeletedEvent[AbotypId]]("delete-abotyp")
  
  implicit val insertKundePersister = persister[EntityInsertedEvent[KundeModify]]("insert-kunde")
  implicit val updateKundePersister = persister[EntityUpdatedEvent[KundeId, KundeModify]]("update-kunde")
  implicit val deleteKundePersister = persister[EntityDeletedEvent[KundeId]]("delete-kunde")
  
  implicit val insertVertriebsartDLPersister = persister[EntityInsertedEvent[DepotlieferungAbotypModify]]("insert-depotlieferung")
  implicit val updateVertriebsartDLPersister = persister[EntityUpdatedEvent[VertriebsartId, DepotlieferungAbotypModify]]("update-depolieferung")
  implicit val insertVertriebsartPLPersister = persister[EntityInsertedEvent[PostlieferungAbotypModify]]("insert-postlieferung")
  implicit val updateVertriebsartPLPersister = persister[EntityUpdatedEvent[VertriebsartId, PostlieferungAbotypModify]]("update-postlieferung")
  implicit val insertVertriebsartHLPersister = persister[EntityInsertedEvent[HeimlieferungAbotypModify]]("insert-heimlieferung")
  implicit val updateVertriebsartHLPersister = persister[EntityUpdatedEvent[VertriebsartId, HeimlieferungAbotypModify]]("update-heimlieferung")
  implicit val deleteVertriebsartPersister = persister[EntityDeletedEvent[VertriebsartId]]("delete-vertriebsart")
  
  implicit val insertCustomKundetypPersister = persister[EntityInsertedEvent[CustomKundentypCreate]]("insert-custom-kundetyp")
  implicit val updateCustomKundetypPersister = persister[EntityUpdatedEvent[CustomKundentypId, CustomKundentypModify]]("update-custom-kundetyp")
  implicit val deleteCustomKundetypPersister = persister[EntityDeletedEvent[CustomKundentypId]]("delete-custom-kundetyp")
  
  implicit val insertPendenzPersister = persister[EntityInsertedEvent[PendenzModify]]("insert-pendenz")
  implicit val updatePendenzPersister = persister[EntityUpdatedEvent[PendenzId, PendenzModify]]("update-pendenz")
  implicit val deletePendenzPersister = persister[EntityDeletedEvent[PendenzId]]("delete-pendenz")
  
  implicit val insertLieferungPersister = persister[EntityInsertedEvent[LieferungAbotypCreate]]("insert-lieferung")
  implicit val deleteLieferungPersister = persister[EntityDeletedEvent[LieferungId]]("delete-lieferung")
  
  val stammdatenPersisters = List(insertDepotPersister,
   updateDepotPersister,
   deleteDepotPersister,
   insertAboPersister,
   updateAboPersister,
   deleteAboPersister,
   insertAbotypPersister,
   updateAbotypPersister,
   deleteAbotypPersister,
   insertKundePersister,
   updateKundePersister,
   deleteKundePersister,
   insertVertriebsartDLPersister,
   updateVertriebsartDLPersister,
   insertVertriebsartPLPersister,
   updateVertriebsartPLPersister,
   insertVertriebsartHLPersister,
   updateVertriebsartHLPersister,
   deleteVertriebsartPersister,
   insertCustomKundetypPersister,
   updateCustomKundetypPersister,
   deleteCustomKundetypPersister,
   insertPendenzPersister,
   updatePendenzPersister,
   deletePendenzPersister,
   insertLieferungPersister,
   deleteLieferungPersister)
}