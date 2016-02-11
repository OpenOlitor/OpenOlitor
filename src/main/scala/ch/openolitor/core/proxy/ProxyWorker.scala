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
import scala.concurrent.duration._

object ProxyWorker {
  case class Push(msg: String)
  case class BinaryPush(msg: Array[Byte])

  def props(serverConnection: ActorRef, routeMap:Map[String, MandantSystem], wsHandler: WebsocketHandler) = Props(classOf[ProxyWorker], serverConnection, routeMap, wsHandler)
}

/**
 * This Proxy Worker proxies either websocket messages to a websocket server or
 * normal httprequest to a httpserver
 **/
class ProxyWorker(val serverConnection: ActorRef, val routeMap:Map[String, MandantSystem], val wsHandler:WebsocketHandler) 
  extends HttpServiceActor 
  with websocket.WebSocketServerWorker
  with Proxy{
  //Use system's dispatcher as ExecutionContext
  import context.dispatcher
  
  var url:Option[String]=None
  
  import ProxyWorker._
  var wsClient: WebSocket = wsHandler.wsClient
  
  var cancellable: Option[Cancellable] = None
  
  override def receive = businessLogicNoUpgrade orElse closeLogic orElse other
    
  override def postStop() {
    log.debug(s"onPostStop")
    processCloseDown
    super.postStop()
  } 
  
  def processCloseDown = {
    log.debug(s"processCloseDown")
    if (wsClient.isOpen){ 
      wsClient.close
    }
    cancellable.map(c => if (!c.isCancelled)c.cancel)
    cancellable = None
  }
  
  val other: Receive = {
    case x =>      
      log.error(s"Unmatched request: $x")
  }
  
  val requestContextHandshake: Receive = {
    case ctx: RequestContext =>
      handshaking(ctx.request)    
  }
  
  lazy val textMessageListener = new TextListener {
    override def onMessage(message: String) {
      log.debug(s"Got message from server, process locally:$message")
      message match {
        case "Pong" => 
          log.debug("Received Pong")
          //send as well to client          
        case msg => 
      }
      self ! Push(message)
    }    
     /**
	   * Called when the {@link WebSocket} is opened
  	 */
    override def onOpen {
      log.debug(s"messageListener:onOpen")
    }
    
    /**
	   * Called when the {@link WebSocket} is closed
   	*/
    override def onClose {
      log.debug(s"messageListener:onClose")
    }
    
    /**
	   * Called when the {@link WebSocket} is closed with its assic
  	 */
    override def onClose(code: Int, reason : String) {
      log.debug(s"messageListener:onClose:$code -> $reason")
    }
    
    /**
	   * Called when an unexpected error occurd on a {@link WebSocket}
  	 */
    override def onError(t: Throwable) {
      log.error(s"messageListener:onError", t)
    }
  }
  
  lazy val binaryMessageListener = new BinaryListener {
    
    override def onMessage(message:Array[Byte]) {
      log.debug(s"Got binary message from server, process locally:$message")     
      self ! BinaryPush(message)
    }
    
     /**
	   * Called when the {@link WebSocket} is opened
  	 */
    override def onOpen {
      log.debug(s"binaryMessageListener:onOpen")
    }
    
    /**
	   * Called when the {@link WebSocket} is closed
   	*/
    override def onClose {
      log.debug(s"binaryMessageListener:onClose")
    }
    
    /**
	   * Called when the {@link WebSocket} is closed with its assic
  	 */
    override def onClose(code: Int, reason : String) {
      log.debug(s"binaryMessageListener:onClose:$code -> $reason")
    }
    
    /**
	   * Called when an unexpected error occurd on a {@link WebSocket}
  	 */
    override def onError(t: Throwable) {
      log.error(s"binaryMessageListener:onError", t)
    }
  }
  
  val proxyRoute: Route = {
    path(routeMap / "ws"){ mandant =>
      log.debug(s"Got request to websocket resource, create proxy client for $mandant to ${mandant.config.wsUri}")
      url = Some(mandant.config.wsUri)
      openWsClient
      context become (handshaking orElse closeLogic)
      //grab request from already instanciated requestcontext to perform handshake
      requestContextHandshake      
    }~    
    pathPrefix(routeMap){ mandant =>        
      log.debug(s"proxy service request of mandant:$mandant to ${mandant.config.uri}")
      proxyToUnmatchedPath(mandant.config.uri)(mandant.system)      
    }
  }
  
  def openWsClient:Option[WebSocket] = {    
    url.map{ url => 
      log.debug("Reopen ws connection to server")
      wsClient = wsClient.open(url).listener(textMessageListener).listener(binaryMessageListener)      
      
      //start ping-poing to keep websocket connection alive
      cancellable =
        Some(context.system.scheduler.schedule(90 seconds,
          90 seconds,
          self,
          "Ping"))
          
      wsClient          
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
    case BinaryPush(msg) =>
      log.debug(s"Got message from websocket server, Push to client:$msg")
      send(BinaryFrame(ByteString(msg)))
      // just bounce frames back for Autobahn testsuite
    case x: BinaryFrame =>
      log.debug(s"Got from binary data:$x")
    case x: TextFrame =>
      val msg = x.payload.decodeString("UTF-8")
      log.debug(s"Got message from client, send to websocket service:$msg -> $x")
      wsClient.send(msg)
    case "Ping" => 
      log.debug("Send ping")
      wsClient.send("Ping")
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest => // do something
      log.debug(s"Got http request:$x")      
    case UpgradedToWebSocket => 
      log.debug(s"Upgradet to websocket,isOpen:${wsClient.isOpen}")
    case akka.io.Tcp.Closed => 
      processCloseDown
    case akka.io.Tcp.PeerClosed =>
      processCloseDown
    case x =>
      log.warning(s"Got unmatched message:$x:"+x.getClass)
  }

  /**
   * In case we didn't get a websocket upgrade notification
   **/
  def businessLogicNoUpgrade: Receive = runRoute(proxyRoute) orElse other
}