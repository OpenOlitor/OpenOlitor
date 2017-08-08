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
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts

object OO461_DBScripts extends DefaultDBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"update ende from last midnight to next start of day for depotlieferungabo")
      sql"""update ${depotlieferungAboMapping.table} 
        set ende = TIMESTAMP(DATE_ADD(DATE(ende), INTERVAL 1 DAY)) 
        where TIME(ende) = '22:00:00' OR TIME(ende) = '23:00:00'""".execute.apply()
      logger.debug(s"update start from last midnight to next start of day for depotlieferungabo")
      sql"""update ${depotlieferungAboMapping.table} 
        set start = TIMESTAMP(DATE_ADD(DATE(start), INTERVAL 1 DAY)) 
        where TIME(start) = '22:00:00' OR TIME(start) = '23:00:00'""".execute.apply()

      logger.debug(s"update ende from last midnight to next start of day for heimlieferungabo")
      sql"""update ${heimlieferungAboMapping.table} 
        set ende = TIMESTAMP(DATE_ADD(DATE(ende), INTERVAL 1 DAY)) 
        where TIME(ende) = '22:00:00' OR TIME(ende) = '23:00:00'""".execute.apply()
      logger.debug(s"update start from last midnight to next start of day for heimlieferungabo")
      sql"""update ${heimlieferungAboMapping.table} 
        set start = TIMESTAMP(DATE_ADD(DATE(start), INTERVAL 1 DAY)) 
        where TIME(start) = '22:00:00' OR TIME(start) = '23:00:00'""".execute.apply()

      logger.debug(s"update ende from last midnight to next start of day for postlieferungabo")
      sql"""update ${postlieferungAboMapping.table} 
        set ende = TIMESTAMP(DATE_ADD(DATE(ende), INTERVAL 1 DAY)) 
        where TIME(ende) = '22:00:00' OR TIME(ende) = '23:00:00'""".execute.apply()
      logger.debug(s"update start from last midnight to next start of day for postlieferungabo")
      sql"""update ${postlieferungAboMapping.table} 
        set start = TIMESTAMP(DATE_ADD(DATE(start), INTERVAL 1 DAY)) 
        where TIME(start) = '22:00:00' OR TIME(start) = '23:00:00'""".execute.apply()

      logger.debug(s"update datum from last midnight to next start of day for abwesenheit")
      sql"""update ${abwesenheitMapping.table} 
        set datum = TIMESTAMP(DATE_ADD(DATE(datum), INTERVAL 1 DAY)) 
        where TIME(datum) = '22:00:00' OR TIME(datum) = '23:00:00'""".execute.apply()

      logger.debug(s"update datum from last midnight to next noon of day for lieferung")
      sql"""update ${lieferungMapping.table} 
        set datum = DATE_ADD(TIMESTAMP(DATE_ADD(DATE(datum), INTERVAL 1 DAY)), INTERVAL 12 HOUR) 
        where TIME(datum) = '22:00:00' OR TIME(datum) = '23:00:00'""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}
