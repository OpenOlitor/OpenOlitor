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

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: AbotypId, entity: AbotypModify)                       => updateAbotyp(id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: DepotlieferungAbotypModify)   => updateVertriebsart(id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: HeimlieferungAbotypModify)    => updateVertriebsart(id, entity)
    case EntityUpdatedEvent(meta, id: VertriebsartId, entity: PostlieferungAbotypModify)    => updateVertriebsart(id, entity)
    case EntityUpdatedEvent(meta, id: KundeId, entity: KundeModify)                         => updateKunde(id, entity)
    case EntityUpdatedEvent(meta, id: PendenzId, entity: PendenzModify)                     => updatePendenz(id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: HeimlieferungAboModify)                => updateHeimlieferungAbo(id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: PostlieferungAboModify)                => updatePostlieferungAbo(id, entity)
    case EntityUpdatedEvent(meta, id: AboId, entity: DepotlieferungAboModify)               => updateDepotlieferungAbo(id, entity)
    case EntityUpdatedEvent(meta, id: DepotId, entity: DepotModify)                         => updateDepot(id, entity)
    case EntityUpdatedEvent(meta, id: CustomKundentypId, entity: CustomKundentypModify)     => updateKundentyp(id, entity)
    case EntityUpdatedEvent(meta, id: ProduzentId, entity: ProduzentModify)                 => updateProduzent(id, entity)
    case EntityUpdatedEvent(meta, id: ProduktId, entity: ProduktModify)                     => updateProdukt(id, entity)
    case EntityUpdatedEvent(meta, id: ProduktekategorieId, entity: ProduktekategorieModify) => updateProduktekategorie(id, entity)
    case EntityUpdatedEvent(meta, id: TourId, entity: TourModify)                           => updateTour(id, entity)
    case EntityUpdatedEvent(meta, id: ProjektId, entity: ProjektModify)                     => updateProjekt(id, entity)
    case EntityUpdatedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched update event for id:$id, entity:$entity")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def updateAbotyp(id: AbotypId, update: AbotypModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(abotypMapping, id) map { abotyp =>
        //map all updatable fields
        val copy = copyFrom(abotyp, update)
        writeRepository.updateEntity[Abotyp, AbotypId](copy)
      }
    }
  }

  def updateVertriebsart(id: VertriebsartId, vertriebsart: DepotlieferungAbotypModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotlieferungMapping, id) map { depotlieferung =>
        //map all updatable fields
        val copy = copyFrom(depotlieferung, vertriebsart)
        writeRepository.updateEntity[Depotlieferung, VertriebsartId](copy)
      }
    }
  }

  def updateVertriebsart(id: VertriebsartId, vertriebsart: PostlieferungAbotypModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(postlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        writeRepository.updateEntity[Postlieferung, VertriebsartId](copy)
      }
    }
  }

  def updateVertriebsart(id: VertriebsartId, vertriebsart: HeimlieferungAbotypModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(heimlieferungMapping, id) map { lieferung =>
        //map all updatable fields
        val copy = copyFrom(lieferung, vertriebsart)
        writeRepository.updateEntity[Heimlieferung, VertriebsartId](copy)
      }
    }
  }

  def updateKunde(kundeId: KundeId, update: KundeModify) = {
    logger.debug(s"Update Kunde $kundeId => $update")
    if (update.ansprechpersonen.isEmpty) {
      logger.error(s"Update kunde without ansprechperson:$kundeId, update:$update")
    } else {
      DB autoCommit { implicit session =>
        writeRepository.getById(kundeMapping, kundeId) map { kunde =>
          //map all updatable fields
          val bez = update.bezeichnung.getOrElse(update.ansprechpersonen.head.fullName)
          val copy = copyFrom(kunde, update, "bezeichnung" -> bez, "anzahlPersonen" -> update.ansprechpersonen.length,
            "anzahlPendenzen" -> update.pendenzen.length)
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
                "kundeId" -> kundeId, "kundeBezeichnung" -> kundeBezeichnung)
              logger.debug(s"Create new pendenz on Kunde:$kundeId, data -> $newPendenz")

              writeRepository.insertEntity(newPendenz)
            }
          }
      }

      readRepository.getPersonen(kundeId) map { personen =>
        DB autoCommit { implicit session =>
          update.ansprechpersonen.zipWithIndex.map {
            case (updatePerson, index) =>
              personen.filter(_.sort == index).headOption.map { person =>
                logger.debug(s"Update person with at index:$index, data -> $updatePerson")
                val copy = copyFrom(person, updatePerson, "id" -> person.id)

                writeRepository.updateEntity[Person, PersonId](copy)
              }.getOrElse {
                //not found person for this index, remove
                val personId = PersonId(UUID.randomUUID)
                val newPerson = copyTo[PersonModify, Person](updatePerson, "id" -> personId,
                  "kundeId" -> kundeId,
                  "sort" -> index)
                logger.debug(s"Create new person on Kunde:$kundeId, data -> $newPerson")

                writeRepository.insertEntity(newPerson)
              }
          }

          //delete personen which aren't longer bound to this customer
          personen.filter(p => p.sort > update.ansprechpersonen.size) map { personToDelete =>
            writeRepository.deleteEntity[Person, PersonId](personToDelete.id)
          }
        }
      }
    }
  }

  def updatePendenz(id: PendenzId, update: PendenzModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(pendenzMapping, id) map { pendenz =>
        //map all updatable fields
        val copy = copyFrom(pendenz, update, "id" -> id)
        writeRepository.updateEntity[Pendenz, PendenzId](copy)
      }
    }
  }

  def updateDepotlieferungAbo(id: AboId, update: DepotlieferungAboModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update)
        writeRepository.updateEntity[DepotlieferungAbo, AboId](copy)
      }
    }
  }

  def updatePostlieferungAbo(id: AboId, update: PostlieferungAboModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(postlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update)
        writeRepository.updateEntity[PostlieferungAbo, AboId](copy)
      }
    }
  }

  def updateHeimlieferungAbo(id: AboId, update: HeimlieferungAboModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(heimlieferungAboMapping, id) map { abo =>
        //map all updatable fields
        val copy = copyFrom(abo, update)
        writeRepository.updateEntity[HeimlieferungAbo, AboId](copy)
      }
    }
  }

  def updateDepot(id: DepotId, update: DepotModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(depotMapping, id) map { depot =>
        //map all updatable fields
        val copy = copyFrom(depot, update)
        writeRepository.updateEntity[Depot, DepotId](copy)
      }
    }
  }

  def updateKundentyp(id: CustomKundentypId, update: CustomKundentypModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(customKundentypMapping, id) map { kundentyp =>
        //map all updatable fields
        val copy = copyFrom(kundentyp, update)
        writeRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
      }
    }
  }

  def updateProduzent(id: ProduzentId, update: ProduzentModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(produzentMapping, id) map { produzent =>
        //map all updatable fields
        val copy = copyFrom(produzent, update)
        writeRepository.updateEntity[Produzent, ProduzentId](copy)
      }
    }
  }

  def updateProdukt(id: ProduktId, update: ProduktModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(produktMapping, id) map { produkt =>
        //map all updatable fields
        val copy = copyFrom(produkt, update)
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
                val newProduktProduzent = ProduktProduzent(produktProduzentId, id, prod.id)
                logger.debug(s"Create new ProduktProduzent :$produktProduzentId, data -> $newProduktProduzent")
                writeRepository.insertEntity(newProduktProduzent)
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
                val newProduktProduktekategorie = ProduktProduktekategorie(produktProduktekategorieId, id, kat.id)
                logger.debug(s"Create new ProduktProduktekategorie :produktProduktekategorieId, data -> newProduktProduktekategorie")
                writeRepository.insertEntity(newProduktProduktekategorie)
              case None => logger.debug(s"Produktekategorie was not found with bezeichnung :$updateKategorieBezeichnung")
            }
          }
        }
    }
  }

  def updateProduktekategorie(id: ProduktekategorieId, update: ProduktekategorieModify) = {
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
              val copyProdukt = copyTo[Produkt, Produkt](produkt, "kategorien" -> newKategorien)
              writeRepository.updateEntity[Produkt, ProduktId](copyProdukt)
            }
          }
        
        //map all updatable fields
        val copy = copyFrom(produktekategorie, update)
        writeRepository.updateEntity[Produktekategorie, ProduktekategorieId](copy)
      }
    }
  }

  def updateTour(id: TourId, update: TourModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(tourMapping, id) map { tour =>
        //map all updatable fields
        val copy = copyFrom(tour, update)
        writeRepository.updateEntity[Tour, TourId](copy)
      }
    }
  }
  
  def updateProjekt(id: ProjektId, update: ProjektModify) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(projektMapping, id) map { projekt =>
        //map all updatable fields
        val copy = copyFrom(projekt, update)
        writeRepository.updateEntity[Projekt, ProjektId](copy)
      }
    }
  }
}