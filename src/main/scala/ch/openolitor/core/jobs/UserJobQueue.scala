package ch.openolitor.core.jobs

import akka.actor._
import ch.openolitor.core.models.PersonId

object UserJobQueue {
  def props(personId: PersonId): Props = Props(classOf[UserJobQueue], personId)
}

class UserJobQueue(personId: PersonId) extends Actor with ActorLogging {
  def receive: Receive = {
    
  }
}