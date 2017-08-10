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

trait BaseUpdateRepository extends BaseReadRepositorySync with UpdateRepository {
  def updateEntityIf[E <: BaseEntity[I], I <: BaseId](p: (E) => Boolean)(id: I)(updateFieldsHead: (SQLSyntax, SqlValue), updateFieldsTail: (SQLSyntax, SqlValue)*)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {
    modifyEntityIf[E, I](p)(id)(_ => (updateFieldsHead +: updateFieldsTail).toMap)
  }

  def updateEntity[E <: BaseEntity[I], I <: BaseId](id: I)(updateFieldsHead: (SQLSyntax, SqlValue), updateFieldsTail: (SQLSyntax, SqlValue)*)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {
    modifyEntity[E, I](id)(_ => (updateFieldsHead +: updateFieldsTail).toMap)
  }

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](id: I)(updateFunction: (E) => Map[SQLSyntax, SqlValue])(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {
    modifyEntityIf[E, I](_ => true)(id)(updateFunction)
  }

  def modifyEntityIf[E <: BaseEntity[I], I <: BaseId](p: (E) => Boolean)(id: I)(updateFunction: (E) => Map[SQLSyntax, SqlValue])(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {
    getById(syntaxSupport, id) map { orig =>
      if (p(orig)) {
        val alias = syntaxSupport.syntax("x")
        val idAlias = alias.id

        val updateFields = updateFunction(orig) map {
          case (sql, v) => (sql, v.value)
        }

        val allowedFields = syntaxSupport.updateParameters(orig) map (_._1)

        val defaultValues = Map(syntaxSupport.column.modifidat -> parameter(DateTime.now), syntaxSupport.column.modifikator -> parameter(user))

        // filter with allowed fields
        // if there are no modification fields in the passed updateFields default values are used
        val updateParams = (defaultValues ++ updateFields.filterKeys(allowedFields.contains)).toSeq

        logger.debug(s"update entity:${id} with values:$updateParams")

        withSQL(update(syntaxSupport as alias).set(updateParams: _*).where.eq(idAlias, parameter(id))).update.apply()

        //publish event to stream
        val result = getById(syntaxSupport, id).get
        eventPublisher.registerPublish(EntityModified(user, result, orig))
        Some(result)
      } else {
        None
      }
    } getOrElse {
      logger.debug(s"Entity with id:${id} not found, ignore update")
      None
    }
  }
}
