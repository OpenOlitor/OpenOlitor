package ch.openolitor.buchhaltung.eventsourcing

import stamina._

import stamina.json._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol

trait BuchhaltungEventStoreSerializer extends BuchhaltungJsonProtocol with EntityStoreJsonProtocol {
  //V1 persisters
  implicit val rechnungModifyPersister = persister[RechnungModify]("kunde-modify")
  implicit val rechnungIdPersister = persister[RechnungId]("kunde-id")

  val buchhaltungPersisters = List(
    rechnungModifyPersister,
    rechnungIdPersister)
}