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
package ch.openolitor.stammdaten

import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.stammdaten.models.AbotypModify
import ch.openolitor.core.models.PersonId
import scalikejdbc.DBSession
import ch.openolitor.util.ConfigUtil._
import org.joda.time.DateTime
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object StammdatenUpdateService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenUpdateService = new DefaultStammdatenUpdateService(sysConfig, system)
}

class DefaultStammdatenUpdateService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenUpdateService(sysConfig) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Stammdaten Moduls
 */
class StammdatenUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent[_, _]]
    with LazyLogging
    with AsyncConnectionPoolContextAware
    with StammdatenDBMappings
    with LieferungHandler
    with KorbHandler {
  self: StammdatenWriteRepositoryComponent =>

  // Hotfix
  lazy val startTime = DateTime.now.minusSeconds(sysConfig.mandantConfiguration.config.getIntOption("startTimeDelationSeconds") getOrElse 10)

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: VertriebId, entity: VertriebModify) => updateVertrieb(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AbotypId, entity: AbotypModify) => updateAbotyp(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AbotypId, entity: ZusatzAbotypModify) => updateZusatzAbotyp(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: DepotlieferungAbotypModify) => updateDepotlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: HeimlieferungAbotypModify) => updateHeimlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: PostlieferungAbotypModify) => updatePostlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: KundeId, entity: KundeModify) => updateKunde(meta, id, entity)
    case EntityUpdatedEvent(meta, id: PendenzId, entity: PendenzModify) => updatePendenz(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: HeimlieferungAboModify) => updateHeimlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: PostlieferungAboModify) => updatePostlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: DepotlieferungAboModify) => updateDepotlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: ZusatzAboModify) => updateZusatzAboModify(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: AboGuthabenModify) => updateAboGuthaben(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: AboVertriebsartModify) => updateAboVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: DepotId, entity: DepotModify) => updateDepot(meta, id, entity)
    case EntityUpdatedEvent(meta, id: CustomKundentypId, entity: CustomKundentypModify) => updateKundentyp(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduzentId, entity: ProduzentModify) => updateProduzent(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduktId, entity: ProduktModify) => updateProdukt(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduktekategorieId, entity: ProduktekategorieModify) => updateProduktekategorie(meta, id, entity)
    case EntityUpdatedEvent(meta, id: TourId, entity: TourModify) => updateTour(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AuslieferungId, entity: TourAuslieferungModify) => updateAuslieferung(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProjektId, entity: ProjektModify) => updateProjekt(meta, id, entity)
    case EntityUpdatedEvent(meta, id: KontoDatenId, entity: KontoDatenModify) => updateKontoDaten(meta, id, entity)
    case EntityUpdatedEvent(meta, id: LieferungId, entity: Lieferung) => updateLieferung(meta, id, entity)
    case EntityUpdatedEvent(meta, id: LieferungId, entity: LieferungAbgeschlossenModify) => updateLieferungAbgeschlossen(meta, id, entity)
    case EntityUpdatedEvent(meta, id: LieferplanungId, entity: LieferplanungModify) => updateLieferplanung(meta, id, entity)
    case EntityUpdatedEvent(meta, id: LieferungId, lieferpositionen: LieferpositionenModify) =>
      updateLieferpositionen(meta, id, lieferpositionen)
    case EntityUpdatedEvent(meta, id: ProjektVorlageId, entity: ProjektVorlageModify) => updateProjektVorlage(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProjektVorlageId, entity: ProjektVorlageUpload) => updateProjektVorlageDocument(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AuslieferungId, entity: Auslieferung) => updateAuslieferungAusgeliefert(meta, id, entity)
    case EntityUpdatedEvent(meta, id: KorbId, entity: KorbAuslieferungModify) => updateKorbAuslieferungId(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebId, entity: VertriebRecalculationsModify) => updateVertriebRecalculationsModify(meta, id, entity)
    case EntityUpdatedEvent(meta, id: SammelbestellungId, entity: SammelbestellungStatusModify) => updateSammelbestellungStatusModify(meta, id, entity)
    case e =>
  }

  private def updateVertrieb(meta: EventMetadata, id: VertriebId, update: VertriebModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(vertriebMapping, id) map { vertrieb =>
        //map all updatable fields
        val copy = copyFrom(vertrieb, update)
        stammdatenWriteRepository.updateEntityFully[Vertrieb, VertriebId](copy)
      }

      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        stammdatenWriteRepository.updateEntityIf[Lieferung, LieferungId](l => Ungeplant == l.status || Offen == l.status)(lieferung.id)(
          lieferungMapping.column.vertriebBeschrieb -> update.beschrieb
        )
      }

      stammdatenWriteRepository.getAbosByVertrieb(id) map { abo =>
        abo match {
          case dlAbo: DepotlieferungAbo =>
            logger.debug(s"Update abo with data -> vertriebBeschrieb")
            stammdatenWriteRepository.updateEntity[DepotlieferungAbo, AboId](abo.id)(depotlieferungAboMapping.column.vertriebBeschrieb -> update.beschrieb)
          case hlAbo: HeimlieferungAbo =>
            logger.debug(s"Update abo with data -> vertriebBeschrieb")
            stammdatenWriteRepository.updateEntity[HeimlieferungAbo, AboId](abo.id)(heimlieferungAboMapping.column.vertriebBeschrieb -> (update.beschrieb))
          case plAbo: PostlieferungAbo =>
            logger.debug(s"Update abo with data -> vertriebBeschrieb")
            stammdatenWriteRepository.updateEntity[PostlieferungAbo, AboId](abo.id)(postlieferungAboMapping.column.vertriebBeschrieb -> update.beschrieb)
          case zAbo: ZusatzAbo =>
            logger.debug(s"Update abo with data -> vertriebBeschrieb")
            stammdatenWriteRepository.updateEntity[ZusatzAbo, AboId](abo.id)(zusatzAboMapping.column.vertriebBeschrieb -> update.beschrieb)
          case _ =>
        }
      }
    }
  }

  private def updateAbotyp(meta: EventMetadata, id: AbotypId, update: AbotypModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val iabotyp = stammdatenWriteRepository.getAbotypById(id)
      iabotyp match {
        case Some(abotyp: Abotyp) => {
          //map all updatable fields
          val copy = copyFrom(abotyp, update)
          stammdatenWriteRepository.updateEntityFully[Abotyp, AbotypId](copy)
        }
        case _ =>
          throw new IllegalArgumentException("The type of subscription is not known")

          stammdatenWriteRepository.getUngeplanteLieferungen(id) map { lieferung =>
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.id)(lieferungMapping.column.zielpreis -> update.zielpreis)
          }
      }
    }
  }

  def updateZusatzAbotyp(meta: EventMetadata, id: AbotypId, update: ZusatzAbotypModify)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val iabotyp = stammdatenWriteRepository.getAbotypById(id)
      iabotyp match {
        case Some(zusatzabotyp: ZusatzAbotyp) => {
          //map all updatable fields
          val copy = copyFrom(zusatzabotyp, update)
          stammdatenWriteRepository.updateEntityFully[ZusatzAbotyp, AbotypId](copy)
        }
        case _ => throw new IllegalArgumentException("The type of subscription is not known")
      }
    }
  }

  def updateDepotlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: DepotlieferungAbotypModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(depotlieferungMapping, id) map { depotlieferung =>
        //map all updatable fields
        val copy = copyFrom(depotlieferung, vertriebsart)
        stammdatenWriteRepository.updateEntityFully[Depotlieferung, VertriebsartId](copy)
      }
    }
  }

  private def updatePostlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: PostlieferungAbotypModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(postlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        stammdatenWriteRepository.updateEntityFully[Postlieferung, VertriebsartId](copy)
      }
    }
  }

  private def updateHeimlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: HeimlieferungAbotypModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(heimlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        stammdatenWriteRepository.updateEntityFully[Heimlieferung, VertriebsartId](copy)
      }
    }
  }

  private def updateKunde(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit personId: PersonId = meta.originator): Unit = {
    logger.debug(s"Update Kunde $kundeId => $update")
    if (update.ansprechpersonen.isEmpty) {
      logger.error(s"Update kunde without ansprechperson:$kundeId, update:$update")
    } else {
      DB localTxPostPublish { implicit session => implicit publisher =>
        updateKundendaten(meta, kundeId, update)
        updatePersonen(meta, kundeId, update)
        updatePendenzen(meta, kundeId, update)
        updateAbos(meta, kundeId, update)
      }
    }
  }

  private def updateKundendaten(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId = meta.originator): Unit = {
    stammdatenWriteRepository.getById(kundeMapping, kundeId) map { kunde =>
      //map all updatable fields
      val bez = update.bezeichnung.getOrElse(update.ansprechpersonen.head.fullName)
      val copy = copyFrom(kunde, update, "bezeichnung" -> bez, "anzahlPersonen" -> update.ansprechpersonen.length,
        "anzahlPendenzen" -> update.pendenzen.length, "modifidat" -> meta.timestamp, "modifikator" -> personId)
      stammdatenWriteRepository.updateEntityFully[Kunde, KundeId](copy)
    }
  }

  private def updatePersonen(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId = meta.originator): Unit = {
    val personen = stammdatenWriteRepository.getPersonen(kundeId)
    update.ansprechpersonen.zipWithIndex.map {
      case (updatePerson, index) =>
        updatePerson.id.map { id =>
          personen.filter(_.id == id).headOption.map { person =>
            logger.debug(s"Update person with at index:$index, data -> $updatePerson")
            val copy = copyFrom(person, updatePerson, "id" -> person.id, "modifidat" -> meta.timestamp, "modifikator" -> personId)

            stammdatenWriteRepository.updateEntityFully[Person, PersonId](copy)
          }
        }
    }
  }

  private def updatePendenzen(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId = meta.originator): Unit = {
    val pendenzen = stammdatenWriteRepository.getPendenzen(kundeId)
    update.pendenzen.map {
      case updatePendenz =>
        updatePendenz.id.map { id =>
          pendenzen.filter(_.id == id).headOption.map { pendenz =>
            logger.debug(s"Update pendenz with data -> updatePendenz")
            val copy = copyFrom(pendenz, updatePendenz, "id" -> pendenz.id, "modifidat" -> meta.timestamp, "modifikator" -> personId)

            stammdatenWriteRepository.updateEntityFully[Pendenz, PendenzId](copy)
          }
        }
    }
  }

  private def updateAbos(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId = meta.originator): Unit = {
    val kundeBez = update.ansprechpersonen.size match {
      case 1 => update.ansprechpersonen.head.fullName
      case _ => update.bezeichnung.getOrElse("")
    }

    stammdatenWriteRepository.getKundeDetail(kundeId) map { kunde =>
      kunde.abos.map { updateAbo =>
        updateAbo match {
          case dlAbo: DepotlieferungAbo =>
            logger.debug(s"Update abo with data -> kundeBez")
            stammdatenWriteRepository.updateEntity[DepotlieferungAbo, AboId](dlAbo.id)(depotlieferungAboMapping.column.kunde -> kundeBez)
          case hlAbo: HeimlieferungAbo =>
            logger.debug(s"Update abo with data -> kundeBez")
            stammdatenWriteRepository.updateEntity[HeimlieferungAbo, AboId](hlAbo.id)(heimlieferungAboMapping.column.kunde -> kundeBez)
          case plAbo: PostlieferungAbo =>
            logger.debug(s"Update abo with data -> kundeBez")
            stammdatenWriteRepository.updateEntity[PostlieferungAbo, AboId](plAbo.id)(postlieferungAboMapping.column.kunde -> kundeBez)
          case zAbo: ZusatzAbo =>
            logger.debug(s"Update abo with data -> kundeBez")
            stammdatenWriteRepository.updateEntity[ZusatzAbo, AboId](zAbo.id)(zusatzAboMapping.column.kunde -> kundeBez)
          case _ =>
        }
      }
    }
  }

  private def updatePendenz(meta: EventMetadata, id: PendenzId, update: PendenzModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(pendenzMapping, id) map { pendenz =>
        //map all updatable fields
        val copy = copyFrom(pendenz, update, "id" -> id, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Pendenz, PendenzId](copy)
      }
    }
  }

  private def updateAuslieferungAusgeliefert(meta: EventMetadata, id: AuslieferungId, update: Auslieferung)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      update match {
        case u: DepotAuslieferung =>
          stammdatenWriteRepository.updateEntityFully[DepotAuslieferung, AuslieferungId](u)
        case u: TourAuslieferung =>
          stammdatenWriteRepository.updateEntityFully[TourAuslieferung, AuslieferungId](u)
        case u: PostAuslieferung =>
          stammdatenWriteRepository.updateEntityFully[PostAuslieferung, AuslieferungId](u)
      }
    }
  }

  private def updateAboGuthaben(meta: EventMetadata, id: AboId, update: AboGuthabenModify)(implicit personId: PersonId = meta.originator): Unit = {
    // Hotfix: only execute in live using the meta info to determine
    if (meta.timestamp.isAfter(this.startTime)) {
      DB localTxPostPublish { implicit session => implicit publisher =>
        stammdatenWriteRepository.getById(depotlieferungAboMapping, id) map { abo =>
          if (abo.guthaben == update.guthabenAlt) {
            val copy = abo.copy(guthaben = update.guthabenNeu)
            stammdatenWriteRepository.updateEntityFully[DepotlieferungAbo, AboId](copy)
            adjustGuthabenVorLieferung(id, update.guthabenNeu)
          }
        }
        stammdatenWriteRepository.getById(heimlieferungAboMapping, id) map { abo =>
          if (abo.guthaben == update.guthabenAlt) {
            val copy = abo.copy(guthaben = update.guthabenNeu)
            stammdatenWriteRepository.updateEntityFully[HeimlieferungAbo, AboId](copy)
            adjustGuthabenVorLieferung(id, update.guthabenNeu)
          }
        }
        stammdatenWriteRepository.getById(postlieferungAboMapping, id) map { abo =>
          if (abo.guthaben == update.guthabenAlt) {
            val copy = abo.copy(guthaben = update.guthabenNeu)
            stammdatenWriteRepository.updateEntityFully[PostlieferungAbo, AboId](copy)
            adjustGuthabenVorLieferung(id, update.guthabenNeu)
          }
        }
      }
    }
  }

  private def adjustGuthabenVorLieferung(id: AboId, guthaben: Int)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Unit = {
    stammdatenWriteRepository.getKoerbeNichtAusgeliefertByAbo(id) map { korb =>
      stammdatenWriteRepository.updateEntity[Korb, KorbId](korb.id)(korbMapping.column.guthabenVorLieferung -> guthaben)
    }
  }

  private def swapOrUpdateAboVertriebsart(meta: EventMetadata, abo: Abo, update: AboVertriebsartModify)(implicit personId: PersonId = meta.originator, session: DBSession, publisher: EventPublisher): Unit = {
    (stammdatenWriteRepository.getById(depotlieferungMapping, update.vertriebsartIdNeu) map { va =>
      stammdatenWriteRepository.getById(depotMapping, va.depotId) map { depot =>
        abo match {
          case abo: DepotlieferungAbo =>
            // wechsel innerhalb selber vertriebart-art
            val copy = abo.copy(vertriebId = va.vertriebId, vertriebsartId = update.vertriebsartIdNeu, depotId = va.depotId, depotName = depot.name)
            stammdatenWriteRepository.updateEntityFully[DepotlieferungAbo, AboId](copy)
          case abo: Abo =>
            // wechsel
            val aboNeu = copyTo[Abo, DepotlieferungAbo](
              abo,
              "depotId" -> va.depotId,
              "depotName" -> depot.name,
              "vertriebId" -> va.vertriebId,
              "vertriebsartId" -> update.vertriebsartIdNeu
            )
            abo match {
              case abo: HeimlieferungAbo => stammdatenWriteRepository.deleteEntity[HeimlieferungAbo, AboId](abo.id)
              case abo: PostlieferungAbo => stammdatenWriteRepository.deleteEntity[PostlieferungAbo, AboId](abo.id)
              case _ =>
            }

            stammdatenWriteRepository.insertEntity[DepotlieferungAbo, AboId](aboNeu)
        }
      }
    }) orElse (stammdatenWriteRepository.getById(heimlieferungMapping, update.vertriebsartIdNeu) map { va =>
      stammdatenWriteRepository.getById(tourMapping, va.tourId) map { tour =>
        abo match {
          case abo: HeimlieferungAbo =>
            // wechsel innerhalb selber vertriebart-art
            val copy = abo.copy(vertriebId = va.vertriebId, vertriebsartId = update.vertriebsartIdNeu, tourId = va.tourId, tourName = tour.name)
            stammdatenWriteRepository.updateEntityFully[HeimlieferungAbo, AboId](copy)
          case abo: Abo =>
            // wechsel
            val aboNeu = copyTo[Abo, HeimlieferungAbo](abo, "vertriebId" -> va.vertriebId, "tourId" -> va.tourId, "tourName" -> tour.name, "vertriebsartId" -> update.vertriebsartIdNeu)
            abo match {
              case abo: DepotlieferungAbo => stammdatenWriteRepository.deleteEntity[DepotlieferungAbo, AboId](abo.id)
              case abo: PostlieferungAbo => stammdatenWriteRepository.deleteEntity[PostlieferungAbo, AboId](abo.id)
              case _ =>
            }
            stammdatenWriteRepository.insertEntity[HeimlieferungAbo, AboId](aboNeu)
        }
      }
    }) orElse (stammdatenWriteRepository.getById(postlieferungMapping, update.vertriebsartIdNeu) map { va =>
      abo match {
        case abo: PostlieferungAbo =>
        // wechsel innerhalb selber vertriebart-art
        // nothing to do
        case abo: Abo =>
          // wechsel
          val aboNeu = copyTo[Abo, PostlieferungAbo](abo, "vertriebId" -> va.vertriebId, "vertriebsartId" -> update.vertriebsartIdNeu)
          abo match {
            case abo: HeimlieferungAbo => stammdatenWriteRepository.deleteEntity[HeimlieferungAbo, AboId](abo.id)
            case abo: DepotlieferungAbo => stammdatenWriteRepository.deleteEntity[DepotlieferungAbo, AboId](abo.id)
            case _ =>
          }
          stammdatenWriteRepository.insertEntity[PostlieferungAbo, AboId](aboNeu)
      }
    })
  }

  private def updateAboVertriebsart(meta: EventMetadata, id: AboId, update: AboVertriebsartModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(depotlieferungAboMapping, id) map { abo =>
        val updatedAbo: Abo = abo.copy(vertriebId = update.vertriebIdNeu, vertriebsartId = update.vertriebsartIdNeu)
        modifyKoerbeForAboVertriebChange(updatedAbo, Some(abo))
        swapOrUpdateAboVertriebsart(meta, abo, update)
      }
      stammdatenWriteRepository.getById(heimlieferungAboMapping, id) map { abo =>
        val updatedAbo: Abo = abo.copy(vertriebId = update.vertriebIdNeu, vertriebsartId = update.vertriebsartIdNeu)
        modifyKoerbeForAboVertriebChange(updatedAbo, Some(abo))
        swapOrUpdateAboVertriebsart(meta, abo, update)
      }
      stammdatenWriteRepository.getById(postlieferungAboMapping, id) map { abo =>
        val updatedAbo: Abo = abo.copy(vertriebId = update.vertriebIdNeu, vertriebsartId = update.vertriebsartIdNeu)
        modifyKoerbeForAboVertriebChange(updatedAbo, Some(abo))
        swapOrUpdateAboVertriebsart(meta, abo, update)
      }
    }
  }

  private def updateDepotlieferungAbo(meta: EventMetadata, id: AboId, update: DepotlieferungAboModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(depotlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val aktiv = IAbo.calculateAktiv(update.start, update.ende)
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> personId, "aktiv" -> aktiv)
        stammdatenWriteRepository.updateEntityFully[DepotlieferungAbo, AboId](copy)

        modifyKoerbeForAboDatumChange(copy, Some(abo))
      }
    }
  }

  def updateZusatzAboModify(meta: EventMetadata, id: AboId, update: ZusatzAboModify)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(zusatzAboMapping, id) map { abo =>
        //map all updatable fields
        val aktiv = IAbo.calculateAktiv(update.start, update.ende)
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> personId, "aktiv" -> aktiv)
        stammdatenWriteRepository.updateEntityFully[ZusatzAbo, AboId](copy)

        modifyKoerbeForAboDatumChange(copy, Some(abo))
      }
    }
  }

  def updatePostlieferungAbo(meta: EventMetadata, id: AboId, update: PostlieferungAboModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(postlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val aktiv = IAbo.calculateAktiv(update.start, update.ende)
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> personId, "aktiv" -> aktiv)
        stammdatenWriteRepository.updateEntityFully[PostlieferungAbo, AboId](copy)

        modifyKoerbeForAboDatumChange(copy, Some(abo))
      }
    }
  }

  private def updateHeimlieferungAbo(meta: EventMetadata, id: AboId, update: HeimlieferungAboModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(heimlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val aktiv = IAbo.calculateAktiv(update.start, update.ende)
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> personId, "aktiv" -> aktiv)
        stammdatenWriteRepository.updateEntityFully[HeimlieferungAbo, AboId](copy)

        modifyKoerbeForAboDatumChange(copy, Some(abo))
      }
    }
  }

  private def updateDepot(meta: EventMetadata, id: DepotId, update: DepotModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(depotMapping, id) map { depot =>
        //map all updatable fields
        val copy = copyFrom(depot, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Depot, DepotId](copy)
      }
    }
  }

  private def updateKundentyp(meta: EventMetadata, id: CustomKundentypId, update: CustomKundentypModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(customKundentypMapping, id) map { kundentyp =>

        // rename kundentyp
        val copy = copyFrom(kundentyp, update, "farbCode" -> "", "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[CustomKundentyp, CustomKundentypId](copy)

        // update typen in Kunde
        stammdatenWriteRepository.getKundenByKundentyp(kundentyp.kundentyp) map { kunde =>
          val newKundentypen = kunde.typen - kundentyp.kundentyp + update.kundentyp
          val newKunde = kunde.copy(
            typen = newKundentypen,
            modifidat = meta.timestamp,
            modifikator = personId
          )

          stammdatenWriteRepository.updateEntityFully[Kunde, KundeId](newKunde)
        }
      }
    }
  }

  private def updateProduzent(meta: EventMetadata, id: ProduzentId, update: ProduzentModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(produzentMapping, id) map { produzent =>
        //map all updatable fields
        val copy = copyFrom(produzent, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Produzent, ProduzentId](copy)
      }
    }
  }

  private def updateProdukt(meta: EventMetadata, id: ProduktId, update: ProduktModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(produktMapping, id) map { produkt =>
        //map all updatable fields
        val copy = copyFrom(produkt, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Produkt, ProduktId](copy)

        //remove all ProduktProduzent-Mappings
        stammdatenWriteRepository.getProduktProduzenten(id) map { produktProduzent =>
          stammdatenWriteRepository.deleteEntity[ProduktProduzent, ProduktProduzentId](produktProduzent.id)
        }
        //recreate new ProduktProduzent-Mappings
        update.produzenten.map { updateProduzentKurzzeichen =>
          val produktProduzentId = ProduktProduzentId(System.currentTimeMillis)
          stammdatenWriteRepository.getProduzentDetailByKurzzeichen(updateProduzentKurzzeichen) match {
            case Some(prod) =>
              val newProduktProduzent = ProduktProduzent(produktProduzentId, id, prod.id, meta.timestamp, personId, meta.timestamp, personId)
              logger.debug(s"Create new ProduktProduzent :$produktProduzentId, data -> $newProduktProduzent")
              stammdatenWriteRepository.insertEntity[ProduktProduzent, ProduktProduzentId](newProduktProduzent)
            case None => logger.debug(s"Produzent was not found with kurzzeichen :$updateProduzentKurzzeichen")
          }
        }

        //remove all ProduktProduktekategorie-Mappings
        stammdatenWriteRepository.getProduktProduktekategorien(id) map { produktProduktekategorien =>
          stammdatenWriteRepository.deleteEntity[ProduktProduktekategorie, ProduktProduktekategorieId](produktProduktekategorien.id)
        }
        //recreate new ProduktProduktekategorie-Mappings
        update.kategorien.map { updateKategorieBezeichnung =>
          val produktProduktekategorieId = ProduktProduktekategorieId(System.currentTimeMillis)
          stammdatenWriteRepository.getProduktekategorieByBezeichnung(updateKategorieBezeichnung) match {
            case Some(kat) =>
              val newProduktProduktekategorie = ProduktProduktekategorie(produktProduktekategorieId, id, kat.id, meta.timestamp, personId, meta.timestamp, personId)
              logger.debug(s"Create new ProduktProduktekategorie :produktProduktekategorieId, data -> newProduktProduktekategorie")
              stammdatenWriteRepository.insertEntity[ProduktProduktekategorie, ProduktProduktekategorieId](newProduktProduktekategorie)
            case None => logger.debug(s"Produktekategorie was not found with bezeichnung :$updateKategorieBezeichnung")
          }
        }
      }
    }
  }

  private def updateProduktekategorie(meta: EventMetadata, id: ProduktekategorieId, update: ProduktekategorieModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(produktekategorieMapping, id) map { produktekategorie =>

        stammdatenWriteRepository.getProdukteByProduktekategorieBezeichnung(produktekategorie.beschreibung) map {
          produkt =>
            //update Produktekategorie-String on Produkt
            val newKategorien = produkt.kategorien map {
              case produktekategorie.beschreibung => update.beschreibung
              case x => x
            }
            val copyProdukt = copyTo[Produkt, Produkt](produkt, "kategorien" -> newKategorien,
              "erstelldat" -> meta.timestamp,
              "ersteller" -> personId,
              "modifidat" -> meta.timestamp,
              "modifikator" -> personId)
            stammdatenWriteRepository.updateEntityFully[Produkt, ProduktId](copyProdukt)
        }

        //map all updatable fields
        val copy = copyFrom(produktekategorie, update)
        stammdatenWriteRepository.updateEntityFully[Produktekategorie, ProduktekategorieId](copy)
      }
    }
  }

  private def updateTourlieferungen(meta: EventMetadata, tourId: TourId, update: TourModify)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId = meta.originator): Unit = {
    update.tourlieferungen.map { tourLieferung =>
      val copy = tourLieferung.copy(modifidat = meta.timestamp, modifikator = meta.originator)
      stammdatenWriteRepository.updateEntityFully[Tourlieferung, AboId](copy)
    }
  }

  private def updateAuslieferung(meta: EventMetadata, auslieferungId: AuslieferungId, update: TourAuslieferungModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(tourAuslieferungMapping, auslieferungId) map { tourAuslieferung =>
        update.koerbe.zipWithIndex.map {
          case (korbModify, index) =>
            stammdatenWriteRepository.getById(korbMapping, korbModify.id) map { korb =>
              val copy = korb.copy(sort = Some(index), modifidat = meta.timestamp, modifikator = meta.originator)
              stammdatenWriteRepository.updateEntityFully[Korb, KorbId](copy)
            }
        }

        // update modifydat
        stammdatenWriteRepository.updateEntityFully[TourAuslieferung, AuslieferungId](tourAuslieferung)
      }
    }
  }

  private def updateTour(meta: EventMetadata, id: TourId, update: TourModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(tourMapping, id) map { tour =>
        //map all updatable fields
        val copy = copyFrom(tour, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Tour, TourId](copy)

        updateTourlieferungen(meta, id, update)
      }
    }
  }

  private def updateProjekt(meta: EventMetadata, id: ProjektId, update: ProjektModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(projektMapping, id) map { projekt =>
        //map all updatable fields
        val copy = copyFrom(projekt, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Projekt, ProjektId](copy)
      }
    }
  }

  private def updateKontoDaten(meta: EventMetadata, id: KontoDatenId, update: KontoDatenModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(kontoDatenMapping, id) map { kontoDaten =>
        //map all updatable fields
        val copy = copyFrom(kontoDaten, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[KontoDaten, KontoDatenId](copy)
      }
    }
  }

  private def updateLieferung(meta: EventMetadata, id: LieferungId, update: Lieferung)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(lieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Lieferung, LieferungId](copy)
      }
    }
  }

  private def updateLieferplanung(meta: EventMetadata, id: LieferplanungId, update: LieferplanungModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        //map all updatable fields
        val copy = copyFrom(lieferplanung, update, "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[Lieferplanung, LieferplanungId](copy)
      }
    }
  }

  @deprecated("Neu werden die Lieferungen mit ihren Lieferpositionen über den Command ModifyLieferplanung erstellt", "1.0.8")
  private def updateLieferpositionen(meta: EventMetadata, lieferungId: LieferungId, positionen: LieferpositionenModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      recreateLieferpositionen(meta, lieferungId, positionen)
    }
  }

  private def updateProjektVorlage(meta: EventMetadata, vorlageId: ProjektVorlageId, update: ProjektVorlageModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(projektVorlageMapping, vorlageId) map { vorlage =>
        val copy = copyFrom(vorlage, update,
          "modifidat" -> meta.timestamp, "modifikator" -> personId)
        stammdatenWriteRepository.updateEntityFully[ProjektVorlage, ProjektVorlageId](copy)
      }
    }
  }

  private def updateProjektVorlageDocument(meta: EventMetadata, vorlageId: ProjektVorlageId, update: ProjektVorlageUpload)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(projektVorlageMapping, vorlageId) map { vorlage =>
        val copy = vorlage.copy(fileStoreId = Some(update.fileStoreId), modifidat = meta.timestamp, modifikator = personId)
        stammdatenWriteRepository.updateEntityFully[ProjektVorlage, ProjektVorlageId](copy)
      }
    }
  }

  private def updateKorbAuslieferungId(meta: EventMetadata, id: KorbId, entity: KorbAuslieferungModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Korb, KorbId](id)(
        korbMapping.column.auslieferungId -> Option(entity.auslieferungId)
      )
    }
  }

  private def updateVertriebRecalculationsModify(meta: EventMetadata, id: VertriebId, entity: VertriebRecalculationsModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(vertriebMapping, id) map { vertrieb =>
        val copy = copyFrom(vertrieb, entity)
        stammdatenWriteRepository.updateEntityFully[Vertrieb, VertriebId](copy)
      }
    }
  }

  private def updateSammelbestellungStatusModify(meta: EventMetadata, id: SammelbestellungId, entity: SammelbestellungStatusModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
        val copy = copyFrom(sammelbestellung, entity)
        stammdatenWriteRepository.updateEntityFully[Sammelbestellung, SammelbestellungId](copy)
      }
    }
  }

  private def updateLieferungAbgeschlossen(meta: EventMetadata, id: LieferungId, entity: LieferungAbgeschlossenModify)(implicit personId: PersonId = meta.originator): Unit = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(lieferungMapping, id) map { lieferung =>
        val copy = copyFrom(lieferung, entity)
        stammdatenWriteRepository.updateEntityFully[Lieferung, LieferungId](copy)
      }
    }
  }
}
