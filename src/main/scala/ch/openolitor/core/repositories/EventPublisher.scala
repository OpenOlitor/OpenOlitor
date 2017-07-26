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
package ch.openolitor.core.repositories

import ch.openolitor.core.EventStream
import scala.collection.mutable.ArrayBuffer
import scalikejdbc.DBSession
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.ConnectionPoolContext

trait EventPublisher {

  def registerPublish(msg: Object): Unit

  def publishEvents(): Unit = {}
}

object InstantEventPublisher {
  def apply(eventStream: EventStream): InstantEventPublisher = {
    new InstantEventPublisher(eventStream)
  }
}

class InstantEventPublisher(eventStream: EventStream) extends EventPublisher {

  override def registerPublish(msg: Object) = {
    eventStream.publish(msg)
  }
}

class PostEventPublisher(eventStream: EventStream) extends EventPublisher with LazyLogging {
  val events = ArrayBuffer.empty[Object]

  override def registerPublish(msg: Object) = {
    events += msg
  }

  override def publishEvents(): Unit = {
    try {
      events map (eventStream.publish)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to publish events after operation. ${e.getMessage}", e)
    }
  }
}

object EventPublishingImplicits {

  implicit class EventPublishingDB(db: DB.type) {

    /**
     * Resulting events will only be published after the whole transaction is complete.
     */
    def localTxPostPublish[A](execution: DBSession => EventPublisher => A)(implicit context: ConnectionPoolContext, eventStream: EventStream): A = {
      val publisher = new PostEventPublisher(eventStream)

      val result = db localTx { session =>
        execution(session)(publisher)
      }

      publisher.publishEvents()

      result
    }

    /**
     * Use this only for single inserts/updates/deletes as it will emit an event for each individual statement.
     *
     * "If a connection is in auto-commit mode, then all its SQL
     * statements will be executed and committed as individual
     * transactions."
     */
    def autoCommitSinglePublish[A](execution: DBSession => EventPublisher => A)(implicit context: ConnectionPoolContext, eventStream: EventStream): A = {
      val publisher = new PostEventPublisher(eventStream)

      val result = db autoCommit { session =>
        execution(session)(publisher)
      }

      publisher.publishEvents()

      result
    }
  }
}
