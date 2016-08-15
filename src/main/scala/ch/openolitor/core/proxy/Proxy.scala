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
package ch.openolitor.core.proxy

import spray.routing._
import spray.http._
import akka.actor._
import akka.io.IO
import spray.can.server.UHttp
import ch.openolitor.core.DefaultRouteService
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.SystemConfig
import scalaz.NonEmptyList
import ch.openolitor.core.Boot.MandantSystem
import org.jfarcand.wcs.WebSocket
import spray.can.Http
import spray.can.websocket._
import spray.can.websocket.WebSocketServerWorker
import com.typesafe.scalalogging.LazyLogging
import akka.util.Timeout
import scala.concurrent.duration._
import spray.io.CommandWrapper

/**
 * Borrowed from:
 * http://www.cakesolutions.net/teamblogs/http-proxy-with-spray
 */
trait Proxy extends LazyLogging {

  private def proxyRequest(updateRequest: RequestContext => HttpRequest)(implicit system: ActorSystem): Route =
    ctx => IO(UHttp)(system) tell (updateRequest(ctx), ctx.responder)

  private def stripHeader(headers: List[HttpHeader] = Nil) =
    headers filterNot (header => filterHostHeader(header) || filterContinueHeader(header))

  private def filterHostHeader(header: HttpHeader) = header is (HttpHeaders.Host.lowercaseName)

  private def filterContinueHeader(header: HttpHeader) = header.name.toLowerCase == HttpHeaders.Expect.lowercaseName

  private val updateUriUnmatchedPath = (ctx: RequestContext, uri: Uri) =>
    uri.withPath(uri.path ++ ctx.unmatchedPath).withQuery(ctx.request.uri.query)

  def updateRequest(uri: Uri, updateUri: (RequestContext, Uri) => Uri): RequestContext => HttpRequest =
    ctx => ctx.request.copy(
      uri = updateUri(ctx, uri),
      headers = stripHeader(ctx.request.headers)
    )

  def proxyToUnmatchedPath(uri: Uri)(implicit system: ActorSystem): Route = proxyRequest(updateRequest(uri, updateUriUnmatchedPath))
}

object ProxyServiceActor {
  def props(mandanten: NonEmptyList[MandantSystem]): Props = Props(classOf[ProxyServiceActor], mandanten)
}

/**
 * Proxy Service which redirects routes matching a mandant key in first row to either
 * the websocket or service redirect url using their actor system
 */
class ProxyServiceActor(mandanten: NonEmptyList[MandantSystem])
    extends Actor
    with ActorLogging
    with HttpService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context

  val routeMap = mandanten.list.map(c => (c.config.key, c)).toList.toMap

  log.debug(s"Configure proxy service for mandanten${routeMap.keySet}")

  val websocketHandler = new WebsocketHandler

  override def receive = {
    // handle every new connection in an own handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()

      val conn = context.actorOf(ProxyWorker.props(serverConnection, routeMap, websocketHandler))
      serverConnection ! Http.Register(conn)
      //set request timeout to infinite, proxy doesn't read request-timeout property from application.conf correctly
      serverConnection ! SetRequestTimeout(Duration.Inf)
  }
}

