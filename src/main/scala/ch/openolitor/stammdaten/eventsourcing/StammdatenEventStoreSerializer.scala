package ch.openolitor.stammdaten.eventsourcing

import stamina._

import stamina.json._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol

trait StammdatenEventStoreSerializer extends StammdatenJsonProtocol with EntityStoreJsonProtocol {  
  //V1 persisters
  implicit val depotModifyPersister = persister[DepotModify]("depot-modify")
  implicit val depotIdPersister = persister[DepotId]("depot-id")
  
  implicit val aboModifyPersister = persister[AboModify]("abo-modify")
  implicit val aboIdPersister = persister[AboId]("abo-id")
  
  implicit val abotypModifyPersister = persister[AbotypModify]("abotyp-modify")
  implicit val abotypIdPersister = persister[AbotypId]("abotyp-id")
  
  implicit val kundeModifyPersister = persister[KundeModify]("kunde-modify")
  implicit val kundeIdPersister = persister[KundeId]("kunde-id")
  
  implicit val vertriebsartDLPersister = persister[DepotlieferungAbotypModify]("depotlieferung-modify")
  implicit val vertriebsartPLPersister = persister[PostlieferungAbotypModify]("postlieferung-modify")
  implicit val vertriebsartHLPersister = persister[HeimlieferungAbotypModify]("heimlieferung-modify")  
  implicit val vertriebsartIdPersister = persister[VertriebsartId]("vertriebsart-id")
  
  implicit val customKundetypCreatePersister = persister[CustomKundentypCreate]("custom-kundetyp-create")
  implicit val customKundetypModifyPersister = persister[CustomKundentypModify]("custom-kundetyp-modify")
  implicit val customKundetypIdPersister = persister[CustomKundentypId]("custom-kundetyp-id")
  
  implicit val pendenzModifyPersister = persister[PendenzModify]("pendenz-modify")
  implicit val pendenzIdPersister = persister[PendenzId]("pendenz-id")
  
  implicit val lieferungAbotypCreatePersister = persister[LieferungAbotypCreate]("lieferung-abotyp-create")
  implicit val lieferungIdPersister = persister[LieferungId]("lieferung-id")
  
  implicit val produktModifyPersister = persister[ProduktModify]("produkt-modify")
  implicit val produktIdPersister = persister[ProduktId]("produkt-id")
  
  implicit val produktkategorieModifyPersister = persister[ProduktekategorieModify]("produktekategorie-modify")
  implicit val produktkategorieIdPersister = persister[ProduktekategorieId]("produktekategorie-id")
  
  implicit val produzentModifyPersister = persister[ProduzentModify]("produzent-modify")
  implicit val produzentIdPersister = persister[ProduzentId]("produzent-id")
  
  implicit val tourModifyPersiter = persister[TourModify]("tour-modify")
  implicit val tourIdPersister = persister[TourId]("tour-id")
  
  implicit val projektModifyPersiter = persister[ProjektModify]("projekt-modify")
  implicit val projektIdPersister = persister[ProjektId]("projekt-id")
  
  val stammdatenPersisters = List(
    depotModifyPersister,
    depotIdPersister,
    aboModifyPersister,
    aboIdPersister,
    abotypModifyPersister,
    abotypIdPersister,
    kundeModifyPersister,
    kundeIdPersister,
    vertriebsartDLPersister,
    vertriebsartPLPersister,
    vertriebsartHLPersister,  
    vertriebsartIdPersister,
    customKundetypCreatePersister,
    customKundetypModifyPersister,
    customKundetypIdPersister,
    pendenzModifyPersister,
    pendenzIdPersister,
    lieferungAbotypCreatePersister,
    lieferungIdPersister,
    produktIdPersister,
    produktModifyPersister,
    produktkategorieModifyPersister,
    produktkategorieIdPersister,
    produzentModifyPersister,
    produzentIdPersister,
    tourModifyPersiter,
    tourIdPersister,
    projektModifyPersiter,
    projektIdPersister
  )
}