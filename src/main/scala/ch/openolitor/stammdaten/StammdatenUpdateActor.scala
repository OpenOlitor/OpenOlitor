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

import akka.persistence.PersistentView
import akka.actor._
import ch.openolitor.core.domain.EntityStore
import scala.concurrent.duration._
import ch.openolitor.stammdaten._

object StammdatenUpdateActor {
  def props(): Props = Props(classOf[DefaultStammdatenUpdateActor])
}

class DefaultStammdatenUpdateActor
  extends StammdatenUpdateActor with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Update Anweisungen innerhalb des Stammdaten Moduls
 */
class StammdatenUpdateActor extends Actor with ActorLogging {
  self: StammdatenRepositoryComponent =>
  import EntityStore._

  val receive: Receive = {
    case EntityUpdatedEvent(meta, entity) =>
      //TODO: implement entity based matching
      log.debug(s"Receive update event for entity:$entity")
  }
}