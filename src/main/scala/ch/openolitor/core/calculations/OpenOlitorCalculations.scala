package ch.openolitor.core.calculations

import akka.actor.Props
import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.calculations.StammdatenCalculations
import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig

object OpenOlitorCalculations {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[OpenOlitorCalculations], sysConfig, system)
}

class OpenOlitorCalculations(sysConfig: SystemConfig, system: ActorSystem) extends BaseCalculationsSupervisor {
  override lazy val calculators = Set(context.actorOf(StammdatenCalculations.props(sysConfig, system)))
}