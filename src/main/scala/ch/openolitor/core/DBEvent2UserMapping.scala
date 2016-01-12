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
import spray.json._
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models._

object DBEvent2UserMapping extends DefaultJsonProtocol {
  def props(): Props = Props(classOf[DBEvent2UserMapping])

  implicit def dbEventCreateWriter[E <: BaseEntity[_ <: BaseId]](implicit writer: JsonWriter[E]) = new RootJsonWriter[DBEvent[E]] {
    def write(obj: DBEvent[E]): JsValue =
      JsObject("type" -> JsString(obj.productPrefix),
        "entity" -> JsString(obj.entity.productPrefix),
        "data" -> writer.write(obj.entity))
  }
}

/**
 * Redirect all dbevents to the client itself
 */
class DBEvent2UserMapping extends Actor with ActorLogging with ClientReceiver {
  import DBEvent2UserMapping._
  import BaseJsonProtocol._
  import StammdatenJsonProtocol._

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
    case e @ EntityModified(userId, entity: Abotyp) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityCreated(userId, entity: Abotyp) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityDeleted(userId, entity: Abotyp) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])

    case e @ EntityModified(userId, entity: Abo) => send(userId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityCreated(userId, entity: Abo) => send(userId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityDeleted(userId, entity: Abo) => send(userId, e.asInstanceOf[DBEvent[Abo]])

    //    case e @ EntityModified(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])
    //    case e @ EntityCreated(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])
    //    case e @ EntityDeleted(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])

    case e @ EntityModified(userId, entity: Kunde) => send(userId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityCreated(userId, entity: Kunde) => send(userId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityDeleted(userId, entity: Kunde) => send(userId, e.asInstanceOf[DBEvent[Kunde]])

    case e @ EntityModified(userId, entity: Depot) => send(userId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityCreated(userId, entity: Depot) => send(userId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityDeleted(userId, entity: Depot) => send(userId, e.asInstanceOf[DBEvent[Depot]])

    case e @ EntityModified(userId, entity: Tour) => send(userId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityCreated(userId, entity: Tour) => send(userId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityDeleted(userId, entity: Tour) => send(userId, e.asInstanceOf[DBEvent[Tour]])

    case x => log.debug(s"receive unknown event $x")
  }
}