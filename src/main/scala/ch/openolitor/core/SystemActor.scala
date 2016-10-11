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
  case class Child(props: Props, name: String)

  def props(airbrakeNotifier: ActorRef)(implicit sysConfig: SystemConfig): Props = Props(classOf[SystemActor], sysConfig, airbrakeNotifier)
}

/**
 * SystemActor wird benutzt, damit die Supervisor Strategy über alle child actors definiert werden kann
 */
class SystemActor(sysConfig: SystemConfig, airbrakeNotifier: ActorRef) extends Actor with ActorLogging {
  import SystemActor._

  log.debug(s"oo-system:SystemActor initialization:$sysConfig")

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e =>
      log.warning(s"Child actor failed:$e")
      airbrakeNotifier ! e
      Restart
  }

  def receive: Receive = {
    case Child(props, name) =>
      log.debug(s"oo-system:Request child actor $name")
      val actorRef = context.actorOf(props, name)

      //return created actor
      sender ! actorRef
    case e =>
      log.debug(s"oo-system:Received unknown event:$e")
  }
}

