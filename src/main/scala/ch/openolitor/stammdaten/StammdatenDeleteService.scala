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

import akka.persistence.PersistentView
import akka.actor._
import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core._
import ch.openolitor.core.models._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import scala.concurrent.ExecutionContext.Implicits.global

object StammdatenDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenDeleteService = new DefaultStammdatenDeleteService(sysConfig, system)
}

class DefaultStammdatenDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenDeleteService(sysConfig: SystemConfig) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Stammdaten Modul
 */
class StammdatenDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent[_]]
    with LazyLogging with AsyncConnectionPoolContextAware with StammdatenDBMappings {
  self: StammdatenWriteRepositoryComponent =>
  import EntityStore._

  val ZERO = 0

  val handle: Handle = {
    case EntityDeletedEvent(meta, id: AbotypId) => deleteAbotyp(meta, id)
    case EntityDeletedEvent(meta, id: AbwesenheitId) => deleteAbwesenheit(meta, id)
    case EntityDeletedEvent(meta, id: PersonId) => deletePerson(meta, id)
    case EntityDeletedEvent(meta, id: PendenzId) => deletePendenz(meta, id)
    case EntityDeletedEvent(meta, id: KundeId) => deleteKunde(meta, id)
    case EntityDeletedEvent(meta, id: DepotId) => deleteDepot(meta, id)
    case EntityDeletedEvent(meta, id: AboId) => deleteAbo(meta, id)
    case EntityDeletedEvent(meta, id: VertriebsartId) => deleteVertriebsart(meta, id)
    case EntityDeletedEvent(meta, id: LieferungId) => deleteLieferung(meta, id)
    case EntityDeletedEvent(meta, id: CustomKundentypId) => deleteKundentyp(meta, id)
    case EntityDeletedEvent(meta, id: ProduktId) => deleteProdukt(meta, id)
    case EntityDeletedEvent(meta, id: ProduktekategorieId) => deleteProduktekategorie(meta, id)
    case EntityDeletedEvent(meta, id: ProduktProduktekategorieId) => deleteProduktProduktekategorie(meta, id)
    case EntityDeletedEvent(meta, id: ProduzentId) => deleteProduzent(meta, id)
    case EntityDeletedEvent(meta, id: TourId) => deleteTour(meta, id)
    case EntityDeletedEvent(meta, id: VertriebId) => deleteVertrieb(meta, id)
    case EntityDeletedEvent(meta, id: LieferplanungId) => deleteLieferplanung(meta, id)
    case EntityDeletedEvent(meta, id: ProjektVorlageId) => deleteProjektVorlage(meta, id)
    case EntityDeletedEvent(meta, id: LieferungOnLieferplanungId) => removeLieferungPlanung(meta, id)
    case e =>
  }

  def deleteAbotyp(meta: EventMetadata, id: AbotypId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Abotyp, AbotypId](id, { abotyp: Abotyp => abotyp.anzahlAbonnenten == 0 && abotyp.anzahlAbonnentenAktiv == 0 })
    }
  }

  def deleteAbwesenheit(meta: EventMetadata, id: AbwesenheitId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Abwesenheit, AbwesenheitId](id)
    }
  }

  def deletePerson(meta: EventMetadata, id: PersonId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Person, PersonId](id)
    }
  }

  def deleteKunde(meta: EventMetadata, kundeId: KundeId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Kunde, KundeId](kundeId, { kunde: Kunde => kunde.anzahlAbos == 0 && kunde.anzahlAbosAktiv == 0 }) match {
        case Some(kunde) =>
          //delete all personen as well
          stammdatenWriteRepository.getPersonen(kundeId).map(person => deletePerson(meta, person.id))
          //delete all pendenzen as well
          stammdatenWriteRepository.getPendenzen(kundeId).map(pendenz => deletePendenz(meta, pendenz.id))
        case None =>
      }

    }
  }

  def deleteDepot(meta: EventMetadata, id: DepotId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Depot, DepotId](id, { depot: Depot => depot.anzahlAbonnenten == 0 && depot.anzahlAbonnentenAktiv == 0 }) map { depot =>
        stammdatenWriteRepository.getDepotlieferung(depot.id) map { dl =>
          stammdatenWriteRepository.deleteEntity[Depotlieferung, VertriebsartId](dl.id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
        }
      }
    }
  }

  def deletePendenz(meta: EventMetadata, id: PendenzId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Pendenz, PendenzId](id)
    }
  }

  def deleteAbo(meta: EventMetadata, id: AboId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[DepotlieferungAbo, AboId](id)
      stammdatenWriteRepository.deleteEntity[HeimlieferungAbo, AboId](id)
      stammdatenWriteRepository.deleteEntity[PostlieferungAbo, AboId](id)
    }
  }

  def deleteKundentyp(meta: EventMetadata, id: CustomKundentypId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[CustomKundentyp, CustomKundentypId](id) match {
        case Some(kundentyp) =>
          if (kundentyp.anzahlVerknuepfungen > 0) {
            //remove kundentyp from kunden
            stammdatenWriteRepository.getKunden.filter(_.typen.contains(kundentyp.kundentyp)).map { kunde =>
              val copy = kunde.copy(typen = kunde.typen - kundentyp.kundentyp)
              stammdatenWriteRepository.updateEntity[Kunde, KundeId](copy)
            }
          }
        case None =>
      }
    }
  }

  def deleteVertriebsart(meta: EventMetadata, id: VertriebsartId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Depotlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
      stammdatenWriteRepository.deleteEntity[Heimlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
      stammdatenWriteRepository.deleteEntity[Postlieferung, VertriebsartId](id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
    }
  }

  def deleteLieferplanung(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      val lieferungenOld = stammdatenWriteRepository.getLieferungen(id)
      stammdatenWriteRepository.deleteEntity[Lieferplanung, LieferplanungId](id, { lieferplanung: Lieferplanung => lieferplanung.status == Offen }) map {
        lieferplanung =>
          stammdatenWriteRepository.getLieferungen(lieferplanung.id) map { lieferung =>
            stammdatenWriteRepository.deleteKoerbe(lieferung.id)
            stammdatenWriteRepository.deleteLieferpositionen(lieferung.id)

            //detach lieferung
            logger.debug("detach Lieferung:$lieferung")
            val copy = lieferung.copy(lieferplanungId = None)
            stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](copy)
          }
      }
    }
  }

  def removeLieferungPlanung(meta: EventMetadata, id: LieferungOnLieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteLieferpositionen(id.getLieferungId())
      stammdatenWriteRepository.getById(lieferungMapping, id.getLieferungId()) map { lieferung =>
        //map all updatable fields
        val copy = copyTo[Lieferung, Lieferung](
          lieferung,
          "durchschnittspreis" -> ZERO,
          "anzahlLieferungen" -> ZERO,
          "anzahlKoerbeZuLiefern" -> ZERO,
          "anzahlAbwesenheiten" -> ZERO,
          "anzahlSaldoZuTief" -> ZERO,
          "lieferplanungId" -> None,
          "status" -> Ungeplant,
          "modifidat" -> meta.timestamp,
          "modifikator" -> personId
        )
        logger.debug(s"Removed lieferung $id from lieferplanung: ${lieferung.lieferplanungId} => $copy")
        stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](copy)
      }
    }

  }

  def deleteLieferung(meta: EventMetadata, id: LieferungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](id, { lieferung: Lieferung => lieferung.lieferplanungId == None })
    }
  }

  def deleteProdukt(meta: EventMetadata, id: ProduktId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produkt, ProduktId](id)
    }
  }

  def deleteProduzent(meta: EventMetadata, id: ProduzentId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produzent, ProduzentId](id)
    }
  }

  def deleteProduktekategorie(meta: EventMetadata, id: ProduktekategorieId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produktekategorie, ProduktekategorieId](id)
    }
  }

  def deleteProduktProduktekategorie(meta: EventMetadata, id: ProduktProduktekategorieId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[ProduktProduktekategorie, ProduktProduktekategorieId](id)
    }
  }

  def deleteTour(meta: EventMetadata, id: TourId)(implicit personId: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.deleteEntity[Tour, TourId](id, { tour: Tour => tour.anzahlAbonnenten == 0 }) map { tour =>
        stammdatenWriteRepository.getHeimlieferung(tour.id) map { hl =>
          stammdatenWriteRepository.deleteEntity[Heimlieferung, VertriebsartId](hl.id, { vertriebsart: Vertriebsart => vertriebsart.anzahlAbos == 0 && vertriebsart.anzahlAbosAktiv == 0 })
        }
      }
    }
  }

  def deleteVertrieb(meta: EventMetadata, id: VertriebId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Vertrieb, VertriebId](id, { vertrieb: Vertrieb => vertrieb.anzahlAbos == 0 && vertrieb.anzahlAbosAktiv == 0 })
    }
  }

  def deleteProjektVorlage(meta: EventMetadata, id: ProjektVorlageId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[ProjektVorlage, ProjektVorlageId](id)
    }
  }
}
