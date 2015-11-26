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
package ch.openolitor.stammdaten.domain.views

import akka.actor._
import akka.persistence.PersistentView
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore
import ch.openolitor.stammdaten._

object StammdatenInsertActor {
  def props(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultStammdatenInsertActor], sysConfig)
}

class DefaultStammdatenInsertActor(sysConfig: SystemConfig)
  extends StammdatenInsertActor(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertActor(override val sysConfig: SystemConfig) extends Actor with ActorLogging with ConnectionPoolContextAware {
  self: StammdatenRepositoryComponent =>
  import EntityStore._

  val receive: Receive = {
    case EntityInsertedEvent(meta, id, entity) =>
      //TODO: implement entity based matching
      log.debug(s"Receive insert event for entity:$entity with id:$id")
  }
}