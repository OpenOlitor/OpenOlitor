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

import ch.openolitor.core.models._
import java.util.UUID
import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import ch.openolitor.core.EventStream
import scala.util._
import ch.openolitor.core.scalax._
import scala.concurrent.Future
import ch.openolitor.core.db.MultipleAsyncConnectionPoolContext
import ch.openolitor.core.db.OOAsyncDB._

trait ReadRepository {
  def getById[E <: BaseEntity[I], I <: BaseId](syntax: BaseEntitySQLSyntaxSupport[E], id: I)(implicit
    session: DBSession,
    binder: Binders[I]): Option[E]

  def getByIds[E <: BaseEntity[I], I <: BaseId](syntax: BaseEntitySQLSyntaxSupport[E], ids: Seq[I])(implicit
    session: DBSession,
    binder: Binders[I]): List[E]
}

trait InsertRepository {
  def insertEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: Binders[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E]
}

trait UpdateRepository {
  def updateEntity[E <: BaseEntity[I], I <: BaseId](id: I)(updateFieldsHead: (SQLSyntax, ParameterBinder), updateFieldsTail: (SQLSyntax, ParameterBinder)*)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: Binders[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E]
}

trait DeleteRepository {
  type Validator[E] = E => Boolean
  val TrueValidator: Validator[Any] = x => true

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Validator[E])(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: Binders[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E]

  def deleteEntity[E <: BaseEntity[I], I <: BaseId](id: I, validator: Option[Validator[E]] = None)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: Binders[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E]
}

trait CrudRepository extends ReadRepository with InsertRepository with UpdateRepository with DeleteRepository

