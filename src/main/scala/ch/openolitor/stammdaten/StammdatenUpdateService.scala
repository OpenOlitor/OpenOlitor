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
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.stammdaten.models.AbotypModify
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.UserId

object StammdatenUpdateService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenUpdateService = new DefaultStammdatenUpdateService(sysConfig, system)
}

class DefaultStammdatenUpdateService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenUpdateService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Stammdaten Moduls
 */
class StammdatenUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: AbotypId, entity: AbotypModify)                       => updateAbotyp(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: DepotlieferungAbotypModify)   => updateDepotlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: HeimlieferungAbotypModify)    => updateHeimlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: PostlieferungAbotypModify)    => updatePostlieferungVertriebsart(meta, id, entity)
    case EntityUpdatedEvent(meta, id: KundeId, entity: KundeModify)                         => updateKunde(meta, id, entity)
    case EntityUpdatedEvent(meta, id: PendenzId, entity: PendenzModify)                     => updatePendenz(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: HeimlieferungAboModify)                => updateHeimlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: PostlieferungAboModify)                => updatePostlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: DepotlieferungAboModify)               => updateDepotlieferungAbo(meta, id, entity)
    case EntityUpdatedEvent(meta, id: DepotId, entity: DepotModify)                         => updateDepot(meta, id, entity)
    case EntityUpdatedEvent(meta, id: CustomKundentypId, entity: CustomKundentypModify)     => updateKundentyp(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduzentId, entity: ProduzentModify)                 => updateProduzent(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduktId, entity: ProduktModify)                     => updateProdukt(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProduktekategorieId, entity: ProduktekategorieModify) => updateProduktekategorie(meta, id, entity)
    case EntityUpdatedEvent(meta, id: TourId, entity: TourModify)                           => updateTour(meta, id, entity)
    case EntityUpdatedEvent(meta, id: ProjektId, entity: ProjektModify)                     => updateProjekt(meta, id, entity)
    case EntityUpdatedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched update event for id:$id, entity:$entity")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def updateAbotyp(meta: EventMetadata, id: AbotypId, update: AbotypModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(abotypMapping, id) map { abotyp =>
        //map all updatable fields
        val copy = copyFrom(abotyp, update)
        writeRepository.updateEntity[Abotyp, AbotypId](copy)
      }
    }
  }

  def updateDepotlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: DepotlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotlieferungMapping, id) map { depotlieferung =>
        //map all updatable fields
        val copy = copyFrom(depotlieferung, vertriebsart)
        writeRepository.updateEntity[Depotlieferung, VertriebsartId](copy)
      }
    }
  }

  def updatePostlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: PostlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(postlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        writeRepository.updateEntity[Postlieferung, VertriebsartId](copy)
      }
    }
  }

  def updateHeimlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: HeimlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(heimlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        writeRepository.updateEntity[Heimlieferung, VertriebsartId](copy)
      }
    }
  }

  def updateKunde(meta: EventMetadata, kundeId: KundeId, update: KundeModify)(implicit userId: UserId = meta.originator) = {
    logger.debug(s"Update Kunde $kundeId => $update")
    if (update.ansprechpersonen.isEmpty) {
      logger.error(s"Update kunde without ansprechperson:$kundeId, update:$update")
    } else {
      DB autoCommit { implicit session =>
        writeRepository.getById(kundeMapping, kundeId) map { kunde =>
          //map all updatable fields
          val bez = update.bezeichnung.getOrElse(update.ansprechpersonen.head.fullName)
          val copy = copyFrom(kunde, update, "bezeichnung" -> bez, "anzahlPersonen" -> update.ansprechpersonen.length,
            "anzahlPendenzen" -> update.pendenzen.length, "modifidat" -> meta.timestamp, "modifikator" -> userId)
          writeRepository.updateEntity[Kunde, KundeId](copy)
        }
      }

      readRepository.getPendenzen(kundeId) map { pendenzen =>
        DB autoCommit { implicit session =>
          //remove existing pendenzen
          pendenzen.map {
            pendenzToDelete =>
              writeRepository.deleteEntity[Pendenz, PendenzId](pendenzToDelete.id)
          }
        }
      } andThen {
        case x =>
          DB autoCommit { implicit session =>
            //recreate submitted pendenzen
            update.pendenzen.map { updatePendenz =>
              val pendenzId = PendenzId(UUID.randomUUID)
              val kundeBezeichnung = update.bezeichnung.getOrElse(update.ansprechpersonen.head.fullName)
              val newPendenz = copyTo[PendenzModify, Pendenz](updatePendenz, "id" -> pendenzId,
                "kundeId" -> kundeId, "kundeBezeichnung" -> kundeBezeichnung, 
                  "erstelldat" -> meta.timestamp, 
                  "ersteller" -> userId, 
                  "modifidat" -> meta.timestamp, 
                  "modifikator" -> userId)
              logger.debug(s"Create new pendenz on Kunde:$kundeId, data -> $newPendenz")

              writeRepository.insertEntity[Pendenz, PendenzId](newPendenz)
            }
          }
      }

      readRepository.getPersonen(kundeId) map { personen =>
        DB autoCommit { implicit session =>
          update.ansprechpersonen.zipWithIndex.map {
            case (updatePerson, index) =>
              updatePerson.id.map { id => 
                personen.filter(_.id == id).headOption.map { person =>
                  logger.debug(s"Update person with at index:$index, data -> $updatePerson")
                  val copy = copyFrom(person, updatePerson, "id" -> person.id, "modifidat" -> meta.timestamp, "modifikator" -> userId)

                  writeRepository.updateEntity[Person, PersonId](copy)
                }                
              }              
          }
        }
      }
    }
  }

  def updatePendenz(meta: EventMetadata, id: PendenzId, update: PendenzModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(pendenzMapping, id) map { pendenz =>
        //map all updatable fields
        val copy = copyFrom(pendenz, update, "id" -> id, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Pendenz, PendenzId](copy)
      }
    }
  }

  def updateDepotlieferungAbo(meta: EventMetadata, id: AboId, update: DepotlieferungAboModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[DepotlieferungAbo, AboId](copy)
      }
    }
  }

  def updatePostlieferungAbo(meta: EventMetadata, id: AboId, update: PostlieferungAboModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(postlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[PostlieferungAbo, AboId](copy)
      }
    }
  }

  def updateHeimlieferungAbo(meta: EventMetadata, id: AboId, update: HeimlieferungAboModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(heimlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[HeimlieferungAbo, AboId](copy)
      }
    }
  }

  def updateDepot(meta: EventMetadata, id: DepotId, update: DepotModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotMapping, id) map { depot =>
        //map all updatable fields
        val copy = copyFrom(depot, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Depot, DepotId](copy)
      }
    }
  }

  def updateKundentyp(meta: EventMetadata, id: CustomKundentypId, update: CustomKundentypModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(customKundentypMapping, id) map { kundentyp =>
        //map all updatable fields
        val copy = copyFrom(kundentyp, update, "farbCode" -> "", "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
      }
    }
  }

  def updateProduzent(meta: EventMetadata, id: ProduzentId, update: ProduzentModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(produzentMapping, id) map { produzent =>
        //map all updatable fields
        val copy = copyFrom(produzent, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Produzent, ProduzentId](copy)
      }
    }
  }

  def updateProdukt(meta: EventMetadata, id: ProduktId, update: ProduktModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(produktMapping, id) map { produkt =>
        //map all updatable fields
        val copy = copyFrom(produkt, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Produkt, ProduktId](copy)
      }
    }

    readRepository.getProduktProduzenten(id) map { produktProduzent =>
      DB autoCommit { implicit session =>
        //remove all ProduktProduzent-Mappings
        produktProduzent.map {
          produktProduzentToDelete =>
            writeRepository.deleteEntity[ProduktProduzent, ProduktProduzentId](produktProduzentToDelete.id)
        }
      }
    } andThen {
      case x =>
        DB autoCommit { implicit session =>
          //recreate new ProduktProduzent-Mappings
          update.produzenten.map { updateProduzentKurzzeichen =>
            val produktProduzentId = ProduktProduzentId(UUID.randomUUID)
            readRepository.getProduzentDetailByKurzzeichen(updateProduzentKurzzeichen) map {
              case Some(prod) => 
                val newProduktProduzent = ProduktProduzent(produktProduzentId, id, prod.id, meta.timestamp, userId, meta.timestamp, userId)
                logger.debug(s"Create new ProduktProduzent :$produktProduzentId, data -> $newProduktProduzent")
                writeRepository.insertEntity[ProduktProduzent, ProduktProduzentId](newProduktProduzent)
              case None => logger.debug(s"Produzent was not found with kurzzeichen :$updateProduzentKurzzeichen")
            }
          }
        }
    }
    
    readRepository.getProduktProduktekategorien(id) map { produktProduktekategorien =>
      DB autoCommit { implicit session =>
        //remove all ProduktProduktekategorie-Mappings
        produktProduktekategorien.map {
          produktProduktekategorieToDelete =>
            writeRepository.deleteEntity[ProduktProduktekategorie, ProduktProduktekategorieId](produktProduktekategorieToDelete.id)
        }
      }
    } andThen {
      case x =>
        DB autoCommit { implicit session =>
          //recreate new ProduktProduktekategorie-Mappings
          update.kategorien.map { updateKategorieBezeichnung =>
            val produktProduktekategorieId = ProduktProduktekategorieId(UUID.randomUUID)
            readRepository.getProduktekategorieByBezeichnung(updateKategorieBezeichnung) map {
              case Some(kat) => 
                val newProduktProduktekategorie = ProduktProduktekategorie(produktProduktekategorieId, id, kat.id, meta.timestamp, userId, meta.timestamp, userId)
                logger.debug(s"Create new ProduktProduktekategorie :produktProduktekategorieId, data -> newProduktProduktekategorie")
                writeRepository.insertEntity[ProduktProduktekategorie, ProduktProduktekategorieId](newProduktProduktekategorie)
              case None => logger.debug(s"Produktekategorie was not found with bezeichnung :$updateKategorieBezeichnung")
            }
          }
        }
    }
  }

  def updateProduktekategorie(meta: EventMetadata, id: ProduktekategorieId, update: ProduktekategorieModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(produktekategorieMapping, id) map { produktekategorie =>

        readRepository.getProdukteByProduktekategorieBezeichnung(produktekategorie.beschreibung) map { 
          produkte => produkte map {
            produkt => 
              //update Produktekategorie-String on Produkt
              val newKategorien = produkt.kategorien map { 
                case produktekategorie.beschreibung => update.beschreibung
                case x => x
              }
              val copyProdukt = copyTo[Produkt, Produkt](produkt, "kategorien" -> newKategorien, 
                  "erstelldat" -> meta.timestamp, 
                  "ersteller" -> userId, 
                  "modifidat" -> meta.timestamp, 
                  "modifikator" -> userId)
              writeRepository.updateEntity[Produkt, ProduktId](copyProdukt)
            }
          }
        
        //map all updatable fields
        val copy = copyFrom(produktekategorie, update)
        writeRepository.updateEntity[Produktekategorie, ProduktekategorieId](copy)
      }
    }
  }

  def updateTour(meta: EventMetadata, id: TourId, update: TourModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(tourMapping, id) map { tour =>
        //map all updatable fields
        val copy = copyFrom(tour, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Tour, TourId](copy)
      }
    }
  }
  
  def updateProjekt(meta: EventMetadata, id: ProjektId, update: ProjektModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(projektMapping, id) map { projekt =>
        //map all updatable fields
        val copy = copyFrom(projekt, update, "modifidat" -> meta.timestamp, "modifikator" -> userId)
        writeRepository.updateEntity[Projekt, ProjektId](copy)
      }
    }
  }
}