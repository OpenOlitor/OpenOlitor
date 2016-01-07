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

object StammdatenDeleteService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenDeleteService = new DefaultStammdatenDeleteService(sysConfig, system)
}

class DefaultStammdatenDeleteService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenDeleteService(sysConfig: SystemConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Delete Anweisungen für das Stammdaten Modul
 */
class StammdatenDeleteService(override val sysConfig: SystemConfig) extends EventService[EntityDeletedEvent]
  with LazyLogging with ConnectionPoolContextAware with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>
  import EntityStore._

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val handle: Handle = {
    case EntityDeletedEvent(meta, id: AbotypId) => deleteAbotyp(id)
    case EntityDeletedEvent(meta, id: PersonId) => deletePerson(id)
    case EntityDeletedEvent(meta, id: DepotId) => deleteDepot(id)
    case EntityDeletedEvent(meta, id: AboId) => deleteAbo(id)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def deleteAbotyp(id: AbotypId) = {
    DB autoCommit { implicit session =>

      writeRepository.deleteEntity[Abotyp, AbotypId](id, { abotyp: Abotyp => abotyp.anzahlAbonnenten == 0 })
    }
  }

  def deletePerson(id: PersonId) = {
    DB autoCommit { implicit session =>
      writeRepository.deleteEntity[Person, PersonId](id, { person: Person => person.anzahlAbos == 0 })
    }
  }

  def deleteDepot(id: DepotId) = {
    DB autoCommit { implicit session =>
      writeRepository.deleteEntity[Depot, DepotId](id, { depot: Depot => depot.anzahlAbonnenten == 0 })
    }
  }

  def deleteAbo(id: AboId) = {
    DB autoCommit { implicit session =>
      writeRepository.deleteEntity[DepotlieferungAbo, AboId](id)
      writeRepository.deleteEntity[HeimlieferungAbo, AboId](id)
      writeRepository.deleteEntity[PostlieferungAbo, AboId](id)
    }
  }
}