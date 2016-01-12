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
class StammdatenUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent] with LazyLogging with AsyncConnectionPoolContextAware with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: AbotypId, entity: AbotypModify) => updateAbotyp(id, entity)
    case EntityUpdatedEvent(meta, id: KundeId, entity: KundeModify) => updateKunde(id, entity)
    case EntityUpdatedEvent(meta, id: DepotId, entity: DepotModify) => updateDepot(id, entity)
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

        //remove all existing vertriebsarten
        writeRepository.removeVertriebsarten(id)

        //reassign vertriebsarten
        writeRepository.attachVertriebsarten(id, update.vertriebsarten)
      }
    }
  }

  def updateKunde(kundeId: KundeId, update: KundeModify) = {
    if (update.ansprechpersonen == 0) {
      //TODO: handle error
    } else {
      DB autoCommit { implicit session =>
        writeRepository.getById(kundeMapping, kundeId) map { kunde =>
          //map all updatable fields
          val bez = update.bezeichnung.getOrElse(update.ansprechpersonen.head.fullName)
          val copy = copyFrom(kunde, update, "bezeichnung" -> bez, "anzahlPersonen" -> update.ansprechpersonen.length)
          writeRepository.updateEntity[Kunde, KundeId](copy)
        }
      }

      readRepository.getPersonen(kundeId) map { personen =>
        DB autoCommit { implicit session =>
          val updatedPersonen = update.ansprechpersonen.zipWithIndex.map {
            case (updatePerson, index) =>
              updatePerson.id.map { id =>
                personen.filter(_.id == id).headOption.map { person =>
                  logger.debug(s"Update person with id:$id, data -> $updatePerson")
                  val copy = copyFrom(person, updatePerson, "id" -> id)

                  writeRepository.updateEntity[Person, PersonId](copy)
                  Some(id)
                }.getOrElse {
                  //id not associated with this customer, don't do anything
                  logger.warn(s"Person with id:$id not found on Kunde $kundeId, ignore")
                  None
                }
              }.getOrElse {
                //create new person
                val personId = PersonId(UUID.randomUUID)
                val newPerson = copyTo[PersonModify, Person](updatePerson, "id" -> personId,
                  "kundeId" -> kundeId,
                  "sort" -> index)
                logger.debug(s"Create new person on Kunde:$kundeId, data -> $newPerson")

                writeRepository.insertEntity(newPerson)
                Some(personId)
              }
          }.flatten

          //delete personen which aren't longer bound to this customer
          personen.filterNot(p => updatedPersonen.contains(p.id)) map { personToDelete =>
            writeRepository.deleteEntity[Person, PersonId](personToDelete.id)
          }
        }
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
}