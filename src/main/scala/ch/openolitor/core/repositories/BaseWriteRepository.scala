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
import scala.reflect.runtime.{ universe => ru }

trait BaseWriteRepository extends CrudRepository
    with BaseReadRepositorySync
    with BaseInsertRepository
    with BaseUpdateRepository
    with BaseDeleteRepository {

  /**
   * Updates the given entity with all its properties.
   * Usually, updating all fields is not required and should be avoided.
   * Use with care.
   */
  def updateEntityFully[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit
    session: DBSession,
    syntaxSupport: BaseEntitySQLSyntaxSupport[E],
    binder: Binders[I],
    user: PersonId,
    eventPublisher: EventPublisher): Option[E] = {

    syntaxSupport.updateParameters(entity) map {
      case (s, v) =>
        (s, v)
    } match {
      case head :: tail =>
        updateEntity[E, I](entity.id)(head, tail: _*)
      case _ =>
        None
    }
  }
}
