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
package ch.openolitor.stammdaten

import akka.actor._

import spray.json._
import scalikejdbc._
import ch.openolitor.core.models._
import ch.openolitor.core.domain._
import ch.openolitor.core.ws._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.db._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.buchhaltung.models._
import ch.openolitor.util.IdUtil
import scala.concurrent.ExecutionContext.Implicits.global;
import org.joda.time.DateTime
import scala.concurrent.Future

trait StammdatenEnityModify {
  this: StammdatenWriteRepositoryComponent =>

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E
  )(implicit session: DBSession, syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], personId: PersonId) = {

    stammdatenWriteRepository.getById(syntax, id) map { result =>
      val copy = mod(result)
      stammdatenWriteRepository.updateEntity[E, I](copy)
    }
  }
}
