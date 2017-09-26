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
package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.stammdaten.models._
import ch.openolitor.reports.ReportsDBMappings

object OO731_Reports {

  val CreateReportsTable = new Script with LazyLogging with ReportsDBMappings {

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
CREATE TABLE `Report` (
  `id` bigint(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `beschreibung` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `query` varchar(5000) COLLATE utf8_unicode_ci DEFAULT NULL,
  `erstelldat` datetime NOT NULL,
  `ersteller` bigint(20) NOT NULL,
  `modifidat` datetime NOT NULL,
  `modifikator` bigint(20) NOT NULL,
  KEY `id_index` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci
""".execute.apply()
      Success(true)
    }
  }

  val scripts = Seq(CreateReportsTable)
}
