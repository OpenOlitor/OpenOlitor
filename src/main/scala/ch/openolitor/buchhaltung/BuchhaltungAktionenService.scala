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
package ch.openolitor.buchhaltung

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import java.util.UUID
import scalikejdbc._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.{ Waehrung, CHF, EUR }
import ch.openolitor.buchhaltung.BuchhaltungCommandHandler._
import ch.openolitor.buchhaltung.models.RechnungModifyBezahlt
import scala.concurrent.Future
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungWriteRepositoryComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungWriteRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object BuchhaltungAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungAktionenService = new DefaultBuchhaltungAktionenService(sysConfig, system)
}

class DefaultBuchhaltungAktionenService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungAktionenService(sysConfig) with DefaultBuchhaltungWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Buchhaltung Modul
 */
class BuchhaltungAktionenService(override val sysConfig: SystemConfig) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings {
  self: BuchhaltungWriteRepositoryComponent =>

  val False = false
  val Zero = 0

  val handle: Handle = {
    case RechnungVerschicktEvent(meta, id: RechnungId) =>
      rechnungVerschicken(meta, id)
    case RechnungMahnungVerschicktEvent(meta, id: RechnungId) =>
      rechnungMahnungVerschicken(meta, id)
    case RechnungBezahltEvent(meta, id: RechnungId, entity: RechnungModifyBezahlt) =>
      rechnungenUndRechnungsPositionBezahlen(meta, id, entity)
    case RechnungStorniertEvent(meta, id: RechnungId) =>
      rechungUndRechnungsPositionenStornieren(meta, id)
    case RechnungDeleteEvent(meta, id: RechnungId) =>
      rechungDelete(meta, id)
    case ZahlungsImportCreatedEvent(meta, entity: ZahlungsImportCreate) =>
      createZahlungsImport(meta, entity)
    case ZahlungsEingangErledigtEvent(meta, entity: ZahlungsEingangModifyErledigt) =>
      zahlungsEingangErledigen(meta, entity)
    case RechnungPDFStoredEvent(meta, rechnungId, fileStoreId) =>
      rechnungPDFStored(meta, rechnungId, fileStoreId)
    case MahnungPDFStoredEvent(meta, rechnungId, fileStoreId) =>
      mahnungPDFStored(meta, rechnungId, fileStoreId)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  private def rechnungPDFStored(meta: EventMetadata, id: RechnungId, fileStoreId: String)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>

      buchhaltungWriteRepository.updateEntity[Rechnung, RechnungId](id)(
        rechnungMapping.column.fileStoreId -> Option(fileStoreId)
      )
    }
  }

  private def mahnungPDFStored(meta: EventMetadata, id: RechnungId, fileStoreId: String)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.modifyEntity[Rechnung, RechnungId](id) { rechnung =>
        Map(rechnungMapping.column.mahnungFileStoreIds -> ((rechnung.mahnungFileStoreIds filterNot (_ == "")) + fileStoreId))
      }
    }
  }

  private def rechnungVerschicken(meta: EventMetadata, id: RechnungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.updateEntityIf[Rechnung, RechnungId](Erstellt == _.status)(id)(
        rechnungMapping.column.status -> Verschickt
      )
    }
  }

  private def rechnungMahnungVerschicken(meta: EventMetadata, id: RechnungId)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.modifyEntityIf[Rechnung, RechnungId](Verschickt == _.status)(id) { rechnung =>
        Map(
          rechnungMapping.column.status -> MahnungVerschickt,
          rechnungMapping.column.anzahlMahnungen -> (rechnung.anzahlMahnungen + 1)
        )
      }
    }
  }

  private def rechnungenUndRechnungsPositionBezahlen(meta: EventMetadata, id: RechnungId, entity: RechnungModifyBezahlt)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      rechnungBezahlenUpdate(id, entity)
    }
  }

  private def rechnungBezahlenUpdate(id: RechnungId, entity: RechnungModifyBezahlt)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Unit = {
    buchhaltungWriteRepository.getById(rechnungMapping, id) map { rechnung =>
      buchhaltungWriteRepository.modifyEntityIf[Rechnung, RechnungId](r => Verschickt == r.status || MahnungVerschickt == r.status)(id) { rechnung =>
        Map(
          rechnungMapping.column.status -> Bezahlt,
          rechnungMapping.column.einbezahlterBetrag -> Option(entity.einbezahlterBetrag),
          rechnungMapping.column.eingangsDatum -> Option(entity.eingangsDatum)
        )
      }.map { _ =>
        val rechnungsPositionen = buchhaltungWriteRepository.getRechnungsPositionenByRechnungsId(rechnung.id)
        rechnungsPositionen.map { rp =>
          buchhaltungWriteRepository.modifyEntity[RechnungsPosition, RechnungsPositionId](rp.id) { r =>
            Map(rechnungsPositionMapping.column.status -> RechnungsPositionStatus.Bezahlt)
          }
        }
      }
    }
  }

  private def rechungDelete(meta: EventMetadata, id: RechnungId)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.deleteEntity[Rechnung, RechnungId](id).map { _ =>
        buchhaltungWriteRepository.getRechnungsPositionenByRechnungsId(id).map { rp =>
          buchhaltungWriteRepository.modifyEntity[RechnungsPosition, RechnungsPositionId](rp.id) { _ =>
            Map(
              rechnungsPositionMapping.column.status -> RechnungsPositionStatus.Offen,
              rechnungsPositionMapping.column.rechnungId -> Option.empty[RechnungId]
            )
          }
        }
      }
    }
  }

  private def rechungUndRechnungsPositionenStornieren(meta: EventMetadata, id: RechnungId)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.updateEntityIf[Rechnung, RechnungId](Bezahlt != _.status)(id)(
        rechnungMapping.column.status -> Storniert
      ).map { _ =>
          buchhaltungWriteRepository.getRechnungsPositionenByRechnungsId(id).map { rp =>
            buchhaltungWriteRepository.modifyEntity[RechnungsPosition, RechnungsPositionId](rp.id) { r =>
              Map(rechnungsPositionMapping.column.status -> RechnungsPositionStatus.Storniert)
            }
          }
        }
    }
  }

  private def createZahlungsImport(meta: EventMetadata, entity: ZahlungsImportCreate)(implicit PersonId: PersonId = meta.originator) = {

    def createZahlungsEingang(zahlungsEingangCreate: ZahlungsEingangCreate)(implicit session: DBSession, publisher: EventPublisher) = {
      val zahlungsEingang = copyTo[ZahlungsEingangCreate, ZahlungsEingang](
        zahlungsEingangCreate,
        "erledigt" -> False,
        "bemerkung" -> None,
        "erstelldat" -> meta.timestamp,
        "ersteller" -> meta.originator,
        "modifidat" -> meta.timestamp,
        "modifikator" -> meta.originator
      )

      buchhaltungWriteRepository.insertEntity[ZahlungsEingang, ZahlungsEingangId](zahlungsEingang)
    }

    val zahlungsImport = copyTo[ZahlungsImportCreate, ZahlungsImport](
      entity,
      "anzahlZahlungsEingaenge" -> entity.zahlungsEingaenge.size,
      "anzahlZahlungsEingaengeErledigt" -> Zero,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB localTxPostPublish { implicit session => implicit publisher =>
      entity.zahlungsEingaenge map { eingang =>
        buchhaltungWriteRepository.getZahlungsEingangByReferenznummer(eingang.referenzNummer) match {
          case Some(existingEingang) =>
            createZahlungsEingang(eingang.copy(rechnungId = existingEingang.rechnungId, status = BereitsVerarbeitet))
          case None =>
            buchhaltungWriteRepository.getRechnungByReferenznummer(eingang.referenzNummer) match {
              case Some(rechnung) =>
                val state = if (rechnung.status == Bezahlt) {
                  BereitsVerarbeitet
                } else if (rechnung.betrag != eingang.betrag) {
                  BetragNichtKorrekt
                } else {
                  Ok
                }
                createZahlungsEingang(eingang.copy(rechnungId = Some(rechnung.id), status = state))
              case None =>
                createZahlungsEingang(eingang.copy(status = ReferenznummerNichtGefunden))
            }
        }
      }
      buchhaltungWriteRepository.insertEntity[ZahlungsImport, ZahlungsImportId](zahlungsImport)
    }
  }

  private def zahlungsEingangErledigen(meta: EventMetadata, entity: ZahlungsEingangModifyErledigt)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.modifyEntity[ZahlungsEingang, ZahlungsEingangId](entity.id) { eingang =>
        if (eingang.status == Ok) {
          eingang.rechnungId map { rechnungId =>
            rechnungBezahlenUpdate(rechnungId, RechnungModifyBezahlt(eingang.betrag, eingang.gutschriftsDatum))
          }
        }

        Map(
          zahlungsEingangMapping.column.erledigt -> true,
          zahlungsEingangMapping.column.bemerkung -> entity.bemerkung
        )
      }
    }
  }
}
