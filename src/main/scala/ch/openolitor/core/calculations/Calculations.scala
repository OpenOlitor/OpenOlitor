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
package ch.openolitor.core.calculations

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import akka.actor.Actor
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import akka.actor.Cancellable
import akka.actor.ActorRef
import scala.util.Try

object Calculations {
  case object InitializeCalculation
  case object StartCalculation
  case object CancelScheduledCalculation

  case class CalculationResult(duration: Duration)
}

trait BaseCalculationsSupervisor extends Actor with LazyLogging {
  import Calculations._

  val calculators: Set[ActorRef]

  def receive = {
    case m @ (InitializeCalculation | StartCalculation | CancelScheduledCalculation) =>
      calculators foreach (_ ! m)
    case result @ CalculationResult(duration) =>
      sender ! result
    case _ =>
  }
}

trait BaseCalculation extends Actor with LazyLogging {
  import Calculations._

  var scheduledCalculation: Option[Cancellable] = None

  /**
   * Perform the calculations needed
   */
  protected def calculate(): Unit

  /**
   * Default implementation with calculation starting next midnight
   */
  protected def handleInitialization(): Unit

  def receive = {
    case InitializeCalculation =>
      handleInitialization()
    case StartCalculation =>
      try {
        val start = DateTime.now.getMillis
        calculate()
        val duration = (DateTime.now.getMillis - start) millis

        logger.debug(s"Calculation successful after ${duration}")

        sender ! CalculationResult(duration)
      } catch {
        case e: Exception =>
          logger.error("Calculation failed!", e)
      }
    case CancelScheduledCalculation =>
      scheduledCalculation map (_.cancel())
    case _ =>
  }

  protected def untilNextMidnight: FiniteDuration = {
    val untilNextMidnight = new DateTime().withTimeAtStartOfDay.plusDays(1).getMillis - DateTime.now.getMillis
    untilNextMidnight millis
  }
}
