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
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global

object StammdatenInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenInsertService = new DefaultStammdatenInsertService(sysConfig, system)
}

class DefaultStammdatenInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenInsertService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent] with LazyLogging with AsyncConnectionPoolContextAware
  with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val ZERO = 0

  val handle: Handle = {
    case EntityInsertedEvent(meta, id, abotyp: AbotypModify) =>
      createAbotyp(id, abotyp)
    case EntityInsertedEvent(meta, id, person: PersonModify) =>
      createPerson(id, person)
    case EntityInsertedEvent(meta, id, kunde: KundeModify) =>
      createKunde(id, kunde)
    case EntityInsertedEvent(meta, id, depot: DepotModify) =>
      createDepot(id, depot)
    case EntityInsertedEvent(meta, id, abo: AboModify) =>
      createAbo(id, abo)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def createAbotyp(id: UUID, abotyp: AbotypModify) = {

    val typ = copyTo[AbotypModify, Abotyp](abotyp, "id" -> AbotypId(id).asInstanceOf[AbotypId],
      "anzahlAbonnenten" -> ZERO,
      "letzteLieferung" -> None,
      "waehrung" -> CHF)

    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity(typ)

      //insert vertriebsarten
      writeRepository.attachVertriebsarten(typ.id, abotyp.vertriebsarten)
    }
  }

  def createKunde(id: UUID, create: KundeModify) = {
    val kunde = copyTo[KundeModify, Kunde](create,
      "id" -> KundeId(id).asInstanceOf[KundeId],
      "anzahlAbos" -> ZERO,
      "anzahlPersonen" -> ZERO)
    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity(kunde)
    }
  }

  def createPerson(id: UUID, create: PersonModify) = {
    readRepository.getPersonen(create.kundeId) map { personen =>
      val nextSort = personen.last.sort + 1
      val person = copyTo[PersonModify, Person](create,
        "id" -> PersonId(id).asInstanceOf[PersonId],
        "sort" -> nextSort)
      DB autoCommit { implicit session =>
        writeRepository.insertEntity(person)
      }
    }
  }

  def createDepot(id: UUID, create: DepotModify) = {
    val depot = copyTo[DepotModify, Depot](create,
      "id" -> DepotId(id).asInstanceOf[DepotId],
      "anzahlAbonnenten" -> ZERO)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(depot)
    }
  }

  def createAbo(id: UUID, create: AboModify) = {
    DB autoCommit { implicit session =>
      val abo = create match {
        case create: DepotlieferungAboModify =>
          writeRepository.insertEntity(copyTo[DepotlieferungAboModify, DepotlieferungAbo](create,
            "id" -> AboId(id).asInstanceOf[AboId]))
        case create: HeimlieferungAboModify =>
          writeRepository.insertEntity(copyTo[HeimlieferungAboModify, HeimlieferungAbo](create,
            "id" -> AboId(id).asInstanceOf[AboId]))
        case create: PostlieferungAboModify =>
          writeRepository.insertEntity(copyTo[PostlieferungAboModify, PostlieferungAbo](create,
            "id" -> AboId(id).asInstanceOf[AboId]))
      }
    }
  }
}