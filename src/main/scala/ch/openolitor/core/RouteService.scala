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

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import ch.openolitor.helloworld.HelloWorldRoutes
import ch.openolitor.stammdaten.StammdatenRoutes
import spray.routing.HttpService
import ch.openolitor.stammdaten.DefaultStammdatenRepositoryComponent
import ch.openolitor.core._
import akka.actor.ActorSystem

object RouteServiceActor {
  def props(entityStore: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[RouteServiceActor], entityStore, sysConfig, system)
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class RouteServiceActor(override val entityStore: ActorRef, override val sysConfig: SystemConfig, override val system: ActorSystem)
  extends Actor with ActorReferences
  with DefaultRouteService
  with HelloWorldRoutes
  with StammdatenRoutes
  with DefaultStammdatenRepositoryComponent
  with CORSSupport {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(cors(helloWorldRoute ~ stammdatenRoute))
}

// this trait defines our service behavior independently from the service actor
trait DefaultRouteService extends HttpService {
}