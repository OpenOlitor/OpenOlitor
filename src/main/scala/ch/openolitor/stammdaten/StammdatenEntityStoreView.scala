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

import akka.actor._
import ch.openolitor.core.domain._
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.stammdaten.repositories._

object StammdatenEntityStoreView {
  def props(mailService: ActorRef, entityStore: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenEntityStoreView], mailService, entityStore, sysConfig, system)
}

class DefaultStammdatenEntityStoreView(override val mailService: ActorRef, override val entityStore: ActorRef, implicit val sysConfig: SystemConfig, implicit val system: ActorSystem) extends StammdatenEntityStoreView
  with DefaultStammdatenWriteRepositoryComponent

/**
 * Zusammenfügen des Componenten (cake pattern) zu der persistentView
 */
trait StammdatenEntityStoreView extends EntityStoreView
    with StammdatenEntityStoreViewComponent with ConnectionPoolContextAware {
  self: StammdatenWriteRepositoryComponent =>

  override val module = "stammdaten"

  def initializeEntityStoreView = {
  }
}

/**
 * Instanzieren der jeweiligen Insert, Update und Delete Child Actors
 */
trait StammdatenEntityStoreViewComponent extends EntityStoreViewComponent {
  import EntityStore._
  val mailService: ActorRef
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val insertService = StammdatenInsertService(sysConfig, system)
  override val updateService = StammdatenUpdateService(sysConfig, system)
  override val deleteService = StammdatenDeleteService(sysConfig, system)

  override val aktionenService = StammdatenAktionenService(sysConfig, system, mailService)
}