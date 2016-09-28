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
  protected def handleInitialization(): Unit = {
    scheduledCalculation = Some(context.system.scheduler.schedule(untilNextMidnight, 24 hours)(self ! StartCalculation))
  }

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