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

import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import ch.openolitor.stammdaten.StammdatenDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._
import scala.util.{ Success, Try }
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import scala.collection.Seq

object OO556_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(projektMapping, "welcome_message1", "VARCHAR(2000)", "sprache")
      alterTableAddColumnIfNotExists(projektMapping, "welcome_message2", "VARCHAR(2000)", "welcome_message1")
      alterTableAddColumnIfNotExists(projektMapping, "maintenance_mode", "VARCHAR(1)", "welcome_message2")
      sql"""UPDATE Projekt SET maintenance_mode='0'""".execute.apply()

      Success(true)

    }
  }
  val scripts = Seq(StammdatenScripts)
}
