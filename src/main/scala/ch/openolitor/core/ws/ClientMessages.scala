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
package ch.openolitor.core.ws

import spray.json._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.BaseJsonProtocol

object ClientMessages {
  trait ClientMessage extends Product
  case class ClientMessageWrapper[E <: ClientMessage](msg: E) {
    val `type`: String = msg.productPrefix
  }

  case class HelloClient(personId: PersonId) extends ClientMessage
}

object ClientMessagesJsonProtocol extends BaseJsonProtocol {
  import ClientMessages._

  implicit val helloClientFormat = jsonFormat1(HelloClient.apply)

  implicit def messageWriter[E <: ClientMessage](implicit writer: JsonWriter[E]) = new RootJsonWriter[ClientMessageWrapper[E]] {
    def write(obj: ClientMessageWrapper[E]): JsValue = {
      val fields: Map[String, JsValue] = writer.write(obj.msg).asJsObject.fields + ("type" -> JsString(obj.`type`))
      JsObject(fields)
    }
  }
}