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
package ch.openolitor.reports

import ch.openolitor.core.domain._
import ch.openolitor.core._
import ch.openolitor.core.db.ConnectionPoolContextAware
import akka.actor.Props
import akka.actor.ActorSystem
import ch.openolitor.reports.repositories.DefaultReportsWriteRepositoryComponent
import ch.openolitor.reports.repositories.ReportsWriteRepositoryComponent
import akka.actor.ActorRef

object ReportsEntityStoreView {
  def props(dbEvolutionActor: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultReportsEntityStoreView], dbEvolutionActor, sysConfig, system)
}

class DefaultReportsEntityStoreView(override val dbEvolutionActor: ActorRef, implicit val sysConfig: SystemConfig, implicit val system: ActorSystem) extends ReportsEntityStoreView
  with DefaultReportsWriteRepositoryComponent

/**
 * Zusammenfügen des Componenten (cake pattern) zu der persistentView
 */
trait ReportsEntityStoreView extends EntityStoreView
    with ReportsEntityStoreViewComponent with ConnectionPoolContextAware {
  self: ReportsWriteRepositoryComponent =>

  override val module = "reports"

  def initializeEntityStoreView = {
  }
}

/**
 * Instanzieren der jeweiligen Insert, Update und Delete Child Actors
 */
trait ReportsEntityStoreViewComponent extends EntityStoreViewComponent {
  import EntityStore._
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val insertService = ReportsInsertService(sysConfig, system)
  override val updateService = ReportsUpdateService(sysConfig, system)
  override val deleteService = ReportsDeleteService(sysConfig, system)

  override val aktionenService = ReportsAktionenService(sysConfig, system)
}
