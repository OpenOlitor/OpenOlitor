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
package ch.openolitor.core.calculations

import akka.actor.Props
import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.calculations.StammdatenCalculations
import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig
import akka.actor.ActorRef

object OpenOlitorCalculations {
  def props(entityStore: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[OpenOlitorCalculations], sysConfig, system, entityStore)
}

class OpenOlitorCalculations(sysConfig: SystemConfig, system: ActorSystem, entityStore: ActorRef) extends BaseCalculationsSupervisor {
  override lazy val calculators = Set(context.actorOf(StammdatenCalculations.props(sysConfig, system, entityStore)))
}
