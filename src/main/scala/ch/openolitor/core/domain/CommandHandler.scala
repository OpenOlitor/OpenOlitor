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
package ch.openolitor.core.domain

import scala.util.Try
import ch.openolitor.core.models._
import scala.reflect._
import scala.reflect.runtime.universe.{ Try => TTry, _ }
import com.typesafe.scalalogging.LazyLogging
import scala.util.Success

/**
 * Used for handling custom module specific commands.
 * Validates the preconditions for a given command and returns resulting events.
 *
 * The validation has to be thorough so that resulting persistent events will never result in an exception.
 * An exception thrown by processing a successfully persisted event will cause the persistent view to go
 * into recovery and finally stopping the persistent view actor after the last (maxNumberOfRetries) onRecoveryFailure.
 */
trait CommandHandler extends LazyLogging with Defaults {
  import EntityStore._

  val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]]

  def handleEntityInsert[E <: AnyRef, I <: BaseId: ClassTag](idFactory: IdFactory, transactionMeta: EventTransactionMetadata, entity: E, constructor: Long => I): Try[Seq[ResultingEvent]] = {
    Success(Seq(insertEntityEvent(idFactory, transactionMeta, entity, constructor)))
  }

  def insertEntityEvent[E <: AnyRef, I <: BaseId: ClassTag](idFactory: IdFactory, transactionMeta: EventTransactionMetadata, entity: E, constructor: Long => I): ResultingEvent = {
    val clOf = classTag[I].runtimeClass.asInstanceOf[Class[I]]
    logger.debug(s"created => Insert entity:$entity")
    val id = idFactory.newId[I](constructor)
    EntityInsertEvent[I, E](id, entity)
  }
}

trait IdFactory {
  def newId[I <: BaseId: ClassTag](cons: Long => I): I
}

class EventMetadataFactory(meta: EventTransactionMetadata) {

  var seqNr = 0L

  private def nextSeqNr() = {
    seqNr += 1
    seqNr
  }

  //  def newId[I <: BaseId: ClassTag](cons: Long => I): I = {
  //    val clOf = classTag[I].runtimeClass.asInstanceOf[Class[I]]
  //    cons(idFactory(clOf))
  //  }
  def newMetadata(): EventMetadata = meta.toMetadata(nextSeqNr())
}