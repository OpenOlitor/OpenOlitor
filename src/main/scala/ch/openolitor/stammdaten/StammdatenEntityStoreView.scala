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

import akka.actor.Props
import ch.openolitor.core.domain.EntityStoreView
import ch.openolitor.core.domain.EntityStoreView
import ch.openolitor.stammdaten.domain.views.StammdatenInsertActor
import akka.actor.Actor
import ch.openolitor.core._
import ch.openolitor.core.domain.EntityStoreViewComponent
import ch.openolitor.stammdaten.domain.views.StammdatenUpdateActor
import ch.openolitor.stammdaten.domain.views.StammdatenDeleteActor
import ch.openolitor.core.domain.EntityStoreView
import ch.openolitor.core.db.ConnectionPoolContextAware

object StammdatenEntityStoreView {
  def props(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultStammdatenEntityStoreView], sysConfig)
}

class DefaultStammdatenEntityStoreView(implicit val sysConfig: SystemConfig) extends StammdatenEntityStoreView
  with DefaultStammdatenRepositoryComponent

/**
 * Zusammenfügen des Componenten (cake pattern) zu der persistentView
 */
trait StammdatenEntityStoreView extends EntityStoreView
  with StammdatenEntityStoreViewComponent with ConnectionPoolContextAware {
  self: StammdatenRepositoryComponent =>

  override val module = "stammdaten"

  def initializeEntityStoreView = {
    writeRepository.cleanupDatabase
  }
}

/**
 * Instanzieren der jeweiligen Insert, Update und Delete Child Actors
 */
trait StammdatenEntityStoreViewComponent extends EntityStoreViewComponent {
  implicit val sysConfig: SystemConfig

  val insertActor = context.actorOf(StammdatenInsertActor.props, "insert-actor")
  val updateActor = context.actorOf(StammdatenUpdateActor.props, "update-actor")
  val deleteActor = context.actorOf(StammdatenDeleteActor.props, "delete-actor")
}