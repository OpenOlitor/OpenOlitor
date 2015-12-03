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
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import ch.openolitor.stammdaten._
import spray.json._

object DBEvent2UserMapping extends DefaultJsonProtocol {
  def props(): Props = Props(classOf[DBEvent2UserMapping])

  import StammdatenJsonProtocol._

  def dbEventCreateWriter[E <: BaseEntity[_ <: BaseId]](implicit writer: JsonWriter[E]) = new RootJsonWriter[DBEvent[E]] {
    def write(obj: DBEvent[E]): JsValue =
      JsObject("type" -> JsString(obj.productPrefix), obj.entity.productPrefix -> writer.write(obj.entity))
  }

  implicit val abotypEventWriter = dbEventCreateWriter[Abotyp]
}

/**
 * Redirect all dbevents to the client itself
 */
class DBEvent2UserMapping extends Actor with ActorLogging with ClientReceiver {
  import DBEvent2UserMapping._
  import BaseJsonProtocol._

  override val system = context.system

  override def preStart() {
    super.preStart()
    //register ourself as listener to sendtoclient commands
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    //TODO: resolve module based json formats of entities, maybe create module based sealed interfaces?
    case e @ EntityModified(userId, entity: Abotyp) =>
      log.debug(s"receive EntityModified $userId, $entity")
      send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityCreated(userId, entity: Abotyp) =>
      log.debug(s"receive EntityCreated $userId, $entity")
      send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityDeleted(userId, entity: Abotyp) =>
      log.debug(s"receive EntityDeleted $userId, $entity")
      send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case x =>
      log.debug(s"receive unknown event $x")
  }
}