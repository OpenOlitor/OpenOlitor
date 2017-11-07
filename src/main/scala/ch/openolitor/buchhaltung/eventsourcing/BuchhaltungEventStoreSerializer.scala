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
package ch.openolitor.buchhaltung.eventsourcing

import spray.json.DefaultJsonProtocol
import stamina._
import stamina.json._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler._
import zangelo.spray.json.AutoProductFormats
import ch.openolitor.core.JSONSerializable

trait BuchhaltungEventStoreSerializer extends BuchhaltungJsonProtocol with EntityStoreJsonProtocol with AutoProductFormats[JSONSerializable] {
  import ch.openolitor.core.eventsourcing.events._

  object MigrationToEmpty extends DefaultJsonProtocol {
    case class Empty() extends JSONSerializable

    val V1toV2toEmpty: JsonMigrator[V2] = from[V1].to[V2] { _ =>
      Empty().toJson
    }
  }

  // No longer used events
  implicit val rechnungCreatePersisterV1 = persister[MigrationToEmpty.Empty, V2]("rechnung-create", MigrationToEmpty.V1toV2toEmpty)

  // V1 persisters
  implicit val rechnungCreatePersisterV2 = persister[RechnungCreateFromRechnungsPositionen]("rechnung-create-from-rechnungsposition")

  implicit val rechnungModifyPersister = persister[RechnungModify]("rechnung-modify")
  implicit val rechnungsPositionCreatePersister = persister[RechnungsPositionCreate]("rechnungs-position-create")
  implicit val rechnungsPositionModifyPersister = persister[RechnungsPositionModify]("rechnungs-position-modify")
  implicit val rechnungsPositionChangeAboIdModifyPersister = persister[RechnungsPositionAssignToRechnung]("rechnungs-position-change-abo-id")
  implicit val rechnungVerschicktEventPersister = persister[RechnungVerschicktEvent, V2]("rechnung-verschickt-event", V1toV2metaDataMigration)
  implicit val rechnungMahnungVerschicktEventPersister = persister[RechnungMahnungVerschicktEvent, V2]("rechnung-mahnung-verschickt-event", V1toV2metaDataMigration)
  implicit val rechnungBezahltEventPersister = persister[RechnungBezahltEvent, V2]("rechnung-bezahlt-event", V1toV2metaDataMigration)
  implicit val rechnungStorniertEventPersister = persister[RechnungStorniertEvent, V2]("rechnung-storniert-event", V1toV2metaDataMigration)
  implicit val rechnungDeleteEventPersister = persister[RechnungDeleteEvent]("rechnung-delete-event")
  implicit val rechnungIdPersister = persister[RechnungId]("rechnung-id")
  implicit val rechnungsPositionIdPersister = persister[RechnungsPositionId]("rechnungs-position-id")

  implicit val zahlungsImportIdPersister = persister[ZahlungsImportId]("zahlungs-import-id")
  implicit val zahlungsImportCreatedEventPersister = persister[ZahlungsImportCreatedEvent, V2]("zahlungs-import-created-event", V1toV2metaDataMigration)
  implicit val zahlungsEingangIdPersister = persister[ZahlungsEingangId]("zahlungs-eingang-id")
  implicit val zahlungsEingangErledigtEventPersister = persister[ZahlungsEingangErledigtEvent, V2]("zahlungs-eingang-erledigt-event", V1toV2metaDataMigration)

  implicit val rechnungPDFStoreEventPersister = persister[RechnungPDFStoredEvent, V2]("rechnung-pdf-stored-event", V1toV2metaDataMigration)
  implicit val mahnungPDFStoreEventPersister = persister[MahnungPDFStoredEvent, V2]("mahnung-pdf-stored-event", V1toV2metaDataMigration)

  val buchhaltungPersisters = List(
    rechnungCreatePersisterV1,
    rechnungCreatePersisterV2,
    rechnungModifyPersister,
    rechnungsPositionCreatePersister,
    rechnungsPositionModifyPersister,
    rechnungsPositionChangeAboIdModifyPersister,
    rechnungIdPersister,
    rechnungsPositionIdPersister,
    rechnungVerschicktEventPersister,
    rechnungMahnungVerschicktEventPersister,
    rechnungBezahltEventPersister,
    rechnungStorniertEventPersister,
    rechnungDeleteEventPersister,
    zahlungsImportIdPersister,
    zahlungsImportCreatedEventPersister,
    zahlungsEingangIdPersister,
    zahlungsEingangErledigtEventPersister,
    rechnungPDFStoreEventPersister,
    mahnungPDFStoreEventPersister
  )
}
