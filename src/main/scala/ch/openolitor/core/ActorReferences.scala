package ch.openolitor.core

import akka.actor.ActorRef

trait ActorReferences {
  val entityStore: ActorRef
}