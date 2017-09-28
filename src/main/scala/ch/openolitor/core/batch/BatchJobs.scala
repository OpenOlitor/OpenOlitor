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
package ch.openolitor.core.batch

import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import akka.actor.Actor
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import akka.actor.Cancellable
import akka.actor.ActorRef
import scala.util.Try

object BatchJobs {
  case object InitializeBatchJob
  case object StartBatchJob
  case object CancelBatchJob

  case class BatchJobResult(duration: Duration)
}

trait BaseBatchJobsSupervisor extends Actor with LazyLogging {
  import BatchJobs._

  val batchJobs: Set[ActorRef]

  def receive = {
    case m @ (InitializeBatchJob | StartBatchJob | CancelBatchJob) =>
      batchJobs foreach (_ ! m)
    case result @ BatchJobResult(duration) =>
      sender ! result
    case _ =>
  }
}

trait BaseBatchJob extends Actor with LazyLogging {
  import BatchJobs._

  var batchJob: Option[Cancellable] = None

  /**
   * Perform the calculations needed
   */
  protected def process(): Unit

  /**
   * Default implementation with calculation starting next midnight
   */
  protected def handleInitialization(): Unit

  def receive = {
    case InitializeBatchJob =>
      handleInitialization()
    case StartBatchJob =>
      try {
        val start = DateTime.now.getMillis
        process()
        val duration = (DateTime.now.getMillis - start) millis

        logger.debug(s"Batch job successful after ${duration}")

        sender ! BatchJobResult(duration)
      } catch {
        case e: Exception =>
          logger.error("Batch job failed!", e)
      }
    case CancelBatchJob =>
      batchJob map (_.cancel())
    case _ =>
  }

  protected def untilNextMidnight: FiniteDuration = {
    val untilNextMidnight = new DateTime().withTimeAtStartOfDay.plusDays(1).getMillis - DateTime.now.getMillis
    untilNextMidnight millis
  }
}
