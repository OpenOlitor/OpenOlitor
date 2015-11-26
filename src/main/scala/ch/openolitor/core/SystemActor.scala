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
package ch.openolitor.core

import akka.actor._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Restart
import scalikejdbc._
import scalikejdbc.config._
import ch.openolitor.core.db._

object SystemActor {
  case class Child(props: Props)

  def props(implicit sysConfig: SystemConfig): Props = Props(classOf[SystemActor], sysConfig)
}

/**
 * SystemActor wird benutzt, damit die Supervisor Strategy über alle child actors definiert werden kann
 */
class SystemActor(sysConfig: SystemConfig) extends Actor with ActorLogging {
  import SystemActor._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
    case _: Exception =>
      Restart
  }

  def receive: Receive = {
    case Child(props) =>
      log.debug(s"Request child actor for props:$props")
      val actorRef = context.actorOf(props)

      //return created actor
      sender ! actorRef
  }
}
