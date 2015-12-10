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
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.stammdaten.models.PostlieferungDetail
import ch.openolitor.stammdaten.models.HeimlieferungDetail
import ch.openolitor.stammdaten.models.DepotlieferungDetail
import ch.openolitor.stammdaten.models.AbotypCreate

object StammdatenInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenInsertService = new DefaultStammdatenInsertService(sysConfig, system)
}

class DefaultStammdatenInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenInsertService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent] with LazyLogging with ConnectionPoolContextAware {
  self: StammdatenRepositoryComponent =>

  val handle: Handle = {
    case EntityInsertedEvent(meta, id, abotyp: AbotypCreate) =>
      insertAbotyp(id, abotyp)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def insertAbotyp(id: UUID, abotyp: AbotypCreate) = {
    val typ = Abotyp(AbotypId(id), abotyp.name, abotyp.beschreibung, abotyp.lieferrhythmus, abotyp.enddatum, abotyp.anzahlLieferungen, abotyp.anzahlAbwesenheiten,
      abotyp.preis, abotyp.preiseinheit, abotyp.aktiv, 0, None)
    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity(typ)

      //insert vertriebsarten
      abotyp.vertriebsarten.map {
        _ match {
          case pd: PostlieferungDetail =>
            writeRepository.insertEntity(Postlieferung(VertriebsartId(), typ.id, pd.liefertage))
          case dd: DepotlieferungDetail =>
            writeRepository.insertEntity(Depotlieferung(VertriebsartId(), typ.id, dd.depot.id, dd.liefertage))
          case hd: HeimlieferungDetail =>
            writeRepository.insertEntity(Heimlieferung(VertriebsartId(), typ.id, hd.tour.id, hd.liefertage))
        }
      }
    }
  }
}