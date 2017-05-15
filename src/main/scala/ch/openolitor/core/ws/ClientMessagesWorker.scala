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
import ch.openolitor.core.models.PersonId
import spray.caching.Cache
import ch.openolitor.core.security.Subject
import scala.concurrent.ExecutionContext.Implicits.global

object ClientMessagesWorker {
  case class Push(receivers: List[PersonId] = Nil, msg: String)

  def props(serverConnection: ActorRef, loginTokenCache: Cache[Subject]) = Props(classOf[ClientMessagesWorker], serverConnection, loginTokenCache)
}
class ClientMessagesWorker(val serverConnection: ActorRef, loginTokenCache: Cache[Subject]) extends HttpServiceActor with websocket.WebSocketServerWorker {

  import ClientMessagesWorker._

  val helloServerPattern = """(.*)("type":\s*"HelloServer")(.*)""".r
  val loginPattern = """\{(.*)("type":"Login"),("token":"([\w|-]+)")(.*)\}""".r
  val logoutPattern = """(.*)("type":"Logout")(.*)""".r
  val clientPingPattern = """(.*)("type":"ClientPing")(.*)""".r
  var personId: Option[PersonId] = None

  def businessLogicLoggedIn: Receive = {
    case Push(Nil, msg) =>
      log.debug(s"Broadcast to client:$msg")
      send(TextFrame(msg))
    case Push(receivers, msg) =>
      personId map { id =>
        receivers.find(_ == id).headOption.map { rec =>
          log.debug(s"Push to client:$msg: $rec")
          send(TextFrame(msg))
        }
      }
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")

      msg match {
        case logoutPattern(_, _, _) =>
          log.debug(s"User logged out from websocket")

          personId = None
          context become businessLogic

          send(TextFrame("""{"type":"LoggedOut"}"""))
        case _ =>
          log.debug(s"Received unknown textframe. State: logged in. $msg")
      }
  }

  def businessLogic: Receive = {

    case x: BinaryFrame =>
      log.debug(s"Got from binary data:$x")
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")

      msg match {
        case helloServerPattern(_, _, _) =>
          send(TextFrame("""{"type":"HelloClient","server":"openolitor"}"""))
        case loginPattern(_, _, _, token, _) =>
          log.debug("Got message token from client: $token")

          loginTokenCache.get(token).map {
            _ map { subject =>
              log.debug(s"User logged in to websocket:$token:${subject.personId}")

              personId = Some(subject.personId)

              context become businessLogicLoggedIn

              send(TextFrame(s"""{"type":"LoggedIn","personId":"${subject.personId.id}"}"""))
            }
          }
        case clientPingPattern(_*) =>
          send(TextFrame(s"""{"type":"ServerPong"}"""))
        case _ =>
          log.debug(s"Received unknown textframe. State: not logged in. $msg")
      }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest => // do something
      log.debug(s"Got http request:$x")
    case x =>
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