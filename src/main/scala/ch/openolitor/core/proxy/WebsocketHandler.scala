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
import akka.actor._
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http._
import spray.can.websocket._
import spray.can.websocket.{ Send, SendStream, UpgradedToWebSocket }
import akka.util.ByteString
import ch.openolitor.core.Boot.MandantSystem
import org.jfarcand.wcs._
import com.ning.http.client._
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.ning.http.client.ws.WebSocketListener
import scala.collection.mutable.ListBuffer
import com.ning.http.client.providers.netty.handler.ConnectionStrategy
import java.util.concurrent.Executors

class WebsocketHandler {

  val wsOptions = new Options
  wsOptions.idleTimeout = 24 * 60 * 60000 // request timeout to whole day
  val wsClient: WebSocket = createWebsocket(wsOptions)

  def createWebsocket(o: Options): WebSocket = {
    val nettyConfig: NettyAsyncHttpProviderConfig = new NettyAsyncHttpProviderConfig
    var asyncHttpClient: AsyncHttpClient = null
    val listeners: ListBuffer[WebSocketListener] = ListBuffer[WebSocketListener]()
    var config = new AsyncHttpClientConfig.Builder

    val executorService = Executors.newFixedThreadPool(3 * Runtime.getRuntime.availableProcessors())

    if (o != null) {
      nettyConfig.setWebSocketMaxBufferSize(o.maxMessageSize)
      nettyConfig.setWebSocketMaxFrameSize(o.maxMessageSize)
      config.setExecutorService(executorService)
        .setRequestTimeout(o.idleTimeout)
        .setConnectTimeout(o.idleTimeout)
        .setConnectionTTL(o.idleTimeout)
        .setPooledConnectionIdleTimeout(o.idleTimeout)
        .setReadTimeout(o.idleTimeout)
        .setWebSocketTimeout(o.idleTimeout)
        .setUserAgent(o.userAgent)
        .setAsyncHttpClientProviderConfig(nettyConfig)
    }

    try {
      config.setFollowRedirect(true)
      asyncHttpClient = new AsyncHttpClient(config.build)
    } catch {
      case t: IllegalStateException => {
        config = new AsyncHttpClientConfig.Builder
      }
    }
    new WebSocket(o, None, false, asyncHttpClient, listeners)
  }
}
