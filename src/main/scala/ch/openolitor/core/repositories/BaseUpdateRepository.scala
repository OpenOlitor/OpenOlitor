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
  /*
   * @param updateFields restrict the updated fields to this list
   */
  def updateEntity[E <: BaseEntity[I], I <: BaseId](entity: E, updateFields: SQLSyntax*)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: SqlBinder[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {
    getById(syntaxSupport, entity.id).map { orig =>
      val alias = syntaxSupport.syntax("x")
      val id = alias.id
      val updateParams = updateFields.toList match {
        case Nil => syntaxSupport.updateParameters(entity)
        case specifiedFields => (syntaxSupport.updateParameters(entity) filter (f => specifiedFields.contains(f._1))) ++ syntaxSupport.defaultColumns(entity)
      }

      logger.debug(s"update entity:${entity.id} with values:$updateParams")

      withSQL(update(syntaxSupport as alias).set(updateParams: _*).where.eq(id, parameter(entity.id))).update.apply()

      //publish event to stream
      eventPublisher.registerPublish(EntityModified(user, entity, orig))

      entity
    } orElse {
      logger.debug(s"Entity with id:${entity.id} not found, ignore update")
      None
    }
  }
}
