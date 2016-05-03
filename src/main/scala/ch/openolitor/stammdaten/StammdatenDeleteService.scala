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
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.stammdaten.models._
import scala.concurrent.ExecutionContext.Implicits.global

object StammdatenDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenDeleteService = new DefaultStammdatenDeleteService(sysConfig, system)
}

class DefaultStammdatenDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenDeleteService(sysConfig: SystemConfig) with DefaultStammdatenWriteRepositoryComponent with DefaultStammdatenReadRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Stammdaten Modul
 */
class StammdatenDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent[_]]
    with LazyLogging with AsyncConnectionPoolContextAware with StammdatenDBMappings {
  self: StammdatenWriteRepositoryComponent with StammdatenReadRepositoryComponent =>
  import EntityStore._

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val handle: Handle = {
    case EntityDeletedEvent(meta, id: AbotypId) => deleteAbotyp(id)
    case EntityDeletedEvent(meta, id: AbwesenheitId) => deleteAbwesenheit(id)
    case EntityDeletedEvent(meta, id: PersonId) => deletePerson(id)
    case EntityDeletedEvent(meta, id: KundeId) => deleteKunde(id)
    case EntityDeletedEvent(meta, id: DepotId) => deleteDepot(id)
    case EntityDeletedEvent(meta, id: AboId) => deleteAbo(id)
    case EntityDeletedEvent(meta, id: VertriebsartId) => deleteVertriebsart(id)
    case EntityDeletedEvent(meta, id: LieferungId) => deleteLieferung(id)
    case EntityDeletedEvent(meta, id: CustomKundentypId) => deleteKundentyp(id)
    case EntityDeletedEvent(meta, id: ProduktId) => deleteProdukt(id)
    case EntityDeletedEvent(meta, id: ProduktekategorieId) => deleteProduktekategorie(id)
    case EntityDeletedEvent(meta, id: ProduktProduktekategorieId) => deleteProduktProduktekategorie(id)
    case EntityDeletedEvent(meta, id: ProduzentId) => deleteProduzent(id)
    case EntityDeletedEvent(meta, id: TourId) => deleteTour(id)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def deleteAbotyp(id: AbotypId) = {
    DB autoCommit { implicit session =>

      stammdatenWriteRepository.deleteEntity[Abotyp, AbotypId](id, { abotyp: Abotyp => abotyp.anzahlAbonnenten == 0 })
    }
  }

  def deleteAbwesenheit(id: AbwesenheitId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Abwesenheit, AbwesenheitId](id, { abw: Abwesenheit =>
        stammdatenWriteRepository.getById(lieferungMapping, abw.lieferungId).map { lieferung: Lieferung =>
          lieferung.lieferplanungId.isEmpty
        }.getOrElse(false)
      })
    }
  }

  def deletePerson(id: PersonId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Person, PersonId](id)
    }
  }

  def deleteKunde(kundeId: KundeId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Kunde, KundeId](kundeId, { kunde: Kunde => kunde.anzahlAbos == 0 }) match {
        case Some(kunde) =>
          //delete all personen as well
          stammdatenReadRepository.getPersonen(kundeId).map(_.map(person => deletePerson(person.id)))
          //delete all pendenzen as well
          stammdatenReadRepository.getPendenzen(kundeId).map(_.map(pendenz => deletePendenz(pendenz.id)))
        case None =>
      }

    }
  }

  def deleteDepot(id: DepotId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Depot, DepotId](id, { depot: Depot => depot.anzahlAbonnenten == 0 })
    }
  }

  def deletePendenz(id: PendenzId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Pendenz, PendenzId](id)
    }
  }

  def deleteAbo(id: AboId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[DepotlieferungAbo, AboId](id)
      stammdatenWriteRepository.deleteEntity[HeimlieferungAbo, AboId](id)
      stammdatenWriteRepository.deleteEntity[PostlieferungAbo, AboId](id)
    }
  }

  def deleteKundentyp(id: CustomKundentypId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[CustomKundentyp, CustomKundentypId](id) match {
        case Some(kundentyp) =>
          if (kundentyp.anzahlVerknuepfungen > 0) {
            //remove kundentyp from kunden
            stammdatenReadRepository.getKunden.map(_.filter(_.typen.contains(kundentyp.kundentyp)).map { kunde =>
              val copy = kunde.copy(typen = kunde.typen - kundentyp.kundentyp)
              stammdatenWriteRepository.updateEntity[Kunde, KundeId](copy)
            })
          }
        case None =>
      }
    }
  }

  def deleteVertriebsart(id: VertriebsartId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Depotlieferung, VertriebsartId](id)
      stammdatenWriteRepository.deleteEntity[Heimlieferung, VertriebsartId](id)
      stammdatenWriteRepository.deleteEntity[Postlieferung, VertriebsartId](id)
    }
  }

  def deleteLieferung(id: LieferungId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Lieferung, LieferungId](id, { lieferung: Lieferung => lieferung.lieferplanungId == None })
    }
  }

  def deleteProdukt(id: ProduktId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produkt, ProduktId](id)
    }
  }

  def deleteProduzent(id: ProduzentId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produzent, ProduzentId](id)
    }
  }

  def deleteProduktekategorie(id: ProduktekategorieId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Produktekategorie, ProduktekategorieId](id)
    }
  }

  def deleteProduktProduktekategorie(id: ProduktProduktekategorieId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[ProduktProduktekategorie, ProduktProduktekategorieId](id)
    }
  }

  def deleteTour(id: TourId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.deleteEntity[Tour, TourId](id)
    }
  }
}