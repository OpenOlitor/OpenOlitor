package ch.openolitor.core

import akka.actor._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Restart

object SystemActor {
  case class Child(props: Props)

  def props(): Props = Props(classOf[SystemActor])
}

/**
 * SystemActor wird benutzt, damit die Supervisor Strategy über alle child actors definiert werden kann
 */
trait SystemActor extends Actor with ActorLogging {
  import SystemActor._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
    case _: Exception => Restart
  }

  def receive: Receive = {
    case Child(props) =>
      log.debug(s"Request child actor for props:$props")
      sender ! context.actorOf(props)
  }
}