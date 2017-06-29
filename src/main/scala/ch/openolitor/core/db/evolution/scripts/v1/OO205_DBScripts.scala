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
package ch.openolitor.core.db.evolution.scripts.v1

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import scala.util.{ Try, Success }

object OO205_DBScripts {

  val StammdatenDBScript = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"alter table ${lieferplanungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${lieferungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${lieferpositionMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${bestellungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${bestellpositionMapping.table}  modify Column id BIGINT".execute.apply()
      sql"alter table ${korbMapping.table} modify Column id BIGINT".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenDBScript)
}