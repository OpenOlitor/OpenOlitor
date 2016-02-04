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

object ProxyWorker {
  case class Push(msg: String)

  def props(serverConnection: ActorRef, routeMap:Map[String, MandantSystem]) = Props(classOf[ProxyWorker], serverConnection, routeMap)
}

/**
 * This Proxy Worker proxies either websocket messages to a websocket server or
 * normal httprequest to a httpserver
 **/
class ProxyWorker(val serverConnection: ActorRef, val routeMap:Map[String, MandantSystem]) 
  extends HttpServiceActor 
  with websocket.WebSocketServerWorker
  with Proxy{

  import ProxyWorker._
  var wsClient:WebSocket
  
  override def receive = businessLogicNoUpgrade orElse closeLogic
  
  val messageListener = new MessageListener {
    override def onMessage(message: String) {
        self ! Push(message)
    }
  }
  
  val proxyRoute: Route = {
    path(routeMap / "ws"){ mandant =>
      log.debug(s"Got request to websocket resource, create proxy client for $mandant to ${mandant.config.wsUri}")
      wsClient = WebSocket().open(mandant.config.wsUri)
      context become (handshaking orElse closeLogic)
      (handshaking orElse closeLogic)
    }~    
    pathPrefix(routeMap){ mandant =>        
      log.debug(s"proxy service request of mandant:$mandant to ${mandant.config.uri}")
      proxyToUnmatchedPath(mandant.config.uri)(mandant.system)      
    }
  }

  /**
   * In case we did get a websocket upgrade
   * Websocket handling logic
   */
  def businessLogic: Receive = {

    case Push(msg) =>
      log.debug(s"Got message from websocket server, Push to client:$msg")
      send(TextFrame(msg))
      // just bounce frames back for Autobahn testsuite
    case x: BinaryFrame =>
      log.debug(s"Got from binary data:$x")
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")
      log.debug(s"Got message from client, send to websocket servier:$x")
      wsClient.send(x.payload.toString)
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest => // do something
      log.debug(s"Got http request:$x")
    case x =>
    case UpgradedToWebSocket => 
      log.debug("Upgradet to websocket, start listening")      
      wsClient.listener(messageListener)      
  }

  /**
   * In case we didn't get a websocket upgrade notification
   **/
  def businessLogicNoUpgrade: Receive = runRoute(proxyRoute)
}