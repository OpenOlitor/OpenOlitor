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
package ch.openolitor.core.db.evolution.scripts

import scalikejdbc._
import com.typesafe.scalalogging.LazyLogging

trait DefaultDBScripts extends LazyLogging {

  /**
   * Helper method to allow easier syntax to add safely column on mariadb server version < 10.0
   */
  def alterTableAddColumnIfNotExists(syntax: SQLSyntaxSupport[_], columnName: String, columnDef: String, after: String)(implicit session: DBSession) = {
    session.execute(s"""SELECT count(*) INTO @exist FROM INFORMATION_SCHEMA.COLUMNS  WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='$columnName' AND TABLE_NAME = '${syntax.tableName}';""")
    session.execute(s"""set @query = IF(@exist <= 0, 'ALTER TABLE ${syntax.tableName} ADD $columnName $columnDef after $after;', 'select 1 status');""")
    session.execute(s"""prepare stmt from @query;""")
    session.execute(s"""EXECUTE stmt;""")
  }
}