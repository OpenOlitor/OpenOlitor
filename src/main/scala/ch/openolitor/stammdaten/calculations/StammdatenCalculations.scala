package ch.openolitor.stammdaten.calculations

import akka.actor.Actor
import akka.actor.ActorLogging
import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import akka.actor.Props
import ch.openolitor.stammdaten.repositories.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.core.calculations.Calculations._
import ch.openolitor.core.calculations.BaseCalculationsSupervisor

object StammdatenCalculations {
  def props(sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenCalculations], sysConfig, system)
}

class StammdatenCalculations(val sysConfig: SystemConfig, val system: ActorSystem) extends BaseCalculationsSupervisor {
  override lazy val calculators = Set(
    context.actorOf(AktiveAbosCalculation.props(sysConfig, system)),
    context.actorOf(KorbStatusCalculation.props(sysConfig, system)),
    context.actorOf(LieferungCounterCalculation.props(sysConfig, system))
  )
}

class DefaultStammdatenCalculations(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCalculations(sysConfig, system) with DefaultStammdatenWriteRepositoryComponent