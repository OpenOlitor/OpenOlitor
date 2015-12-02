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
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.dto._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._

object StammdatenUpdateService {
  def apply(implicit sysConfig: SystemConfig): StammdatenUpdateService = new DefaultStammdatenUpdateService(sysConfig)
}

class DefaultStammdatenUpdateService(sysConfig: SystemConfig)
  extends StammdatenUpdateService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Stammdaten Moduls
 */
class StammdatenUpdateService(override val sysConfig: SystemConfig) extends EventService[EntityUpdatedEvent] with LazyLogging with ConnectionPoolContextAware {
  self: StammdatenRepositoryComponent =>

  val handle: Handle = {
    case EntityUpdatedEvent(meta, id: AbotypId, entity: AbotypUpdate) =>
      updateAbotyp(id, entity)
    case EntityUpdatedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched update event for id:$id, entity:$entity")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def updateAbotyp(id: AbotypId, update: AbotypUpdate) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(Abotyp, id) map { abotyp =>
        //map to abotyp
        val copy = abotyp.copy(name = update.name, beschreibung = update.beschreibung, lieferrhythmus = update.lieferrhythmus,
          enddatum = update.enddatum, anzahlLieferungen = update.anzahlLieferungen, anzahlAbwesenheiten = update.anzahlAbwesenheiten,
          preis = update.preis, preiseinheit = update.preiseinheit, aktiv = update.aktiv, waehrung = update.waehrung)
        writeRepository.updateEntity(copy)

        //TODO: update vertriebsarten mapping
      }
    }
  }
}