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

import spray.routing._
import akka.actor._
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http._
import spray.can.websocket._
import spray.can.websocket.{ Send, SendStream, UpgradedToWebSocket }
import akka.util.ByteString

object ClientMessagesWorker {
  case class Push(msg: String)

  def props(serverConnection: ActorRef) = Props(classOf[ClientMessagesWorker], serverConnection)
}
class ClientMessagesWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {

  import ClientMessagesWorker._

  def businessLogic: Receive = {

    case Push(msg) =>
      log.debug(s"Push to client:$msg")
      send(TextFrame(msg))
    // just bounce frames back for Autobahn testsuite
    case x: BinaryFrame =>
      log.debug(s"Got from binary data:$x")
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")
      log.debug(s"Got from client:$msg")
    //TODO: handle client messages internally
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest => // do something
      log.debug(s"Got http request:$x")
    case x =>
      log.debug(s"Got another message:$x")
      send(TextFrame(s"""{
          "type": "Upgraded", 
          "time": ${System.currentTimeMillis}
      }"""))
  }

  def businessLogicNoUpgrade: Receive = {
    case x =>
      log.debug(s"businessLogicNoUpgrade:$x")
      implicit val refFactory: ActorRefFactory = context
      runRoute {
        getFromResourceDirectory("/")
      }
  }
}