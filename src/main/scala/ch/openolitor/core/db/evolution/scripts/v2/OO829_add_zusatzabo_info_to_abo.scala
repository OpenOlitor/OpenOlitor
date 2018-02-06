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
* the Free Software Foundation, either versiON 3 of the License,              *
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

import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

import scala.util.{ Success, Try }
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts

object OO829_add_zusatzabo_info_to_abo {

  val addZusatzAboInfoToAbo = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      alterTableAddColumnIfNotExists(depotlieferungAboMapping, "zusatz_abo_ids", "VARCHAR(2000)", "aktiv")
      alterTableAddColumnIfNotExists(depotlieferungAboMapping, "zusatz_abotyp_names", "VARCHAR(2000)", "zusatz_abo_ids")

      alterTableAddColumnIfNotExists(heimlieferungAboMapping, "zusatz_abo_ids", "VARCHAR(2000)", "aktiv")
      alterTableAddColumnIfNotExists(heimlieferungAboMapping, "zusatz_abotyp_names", "VARCHAR(2000)", "zusatz_abo_ids")

      alterTableAddColumnIfNotExists(postlieferungAboMapping, "zusatz_abo_ids", "VARCHAR(2000)", "aktiv")
      alterTableAddColumnIfNotExists(postlieferungAboMapping, "zusatz_abotyp_names", "VARCHAR(2000)", "zusatz_abo_ids")

      sql"""update DepotlieferungAbo a join
(SELECT haupt_abo_id, GROUP_CONCAT(z.id) as ids, GROUP_CONCAT(z.abotyp_name) as names from ZusatzAbo z GROUP BY z.haupt_abo_id) pairs
ON a.id = pairs.haupt_abo_id
SET a.zusatz_abo_ids = pairs.ids, a.zusatz_abotyp_names = pairs.names;""".execute.apply()

      sql"""update HeimlieferungAbo a join
(SELECT haupt_abo_id, GROUP_CONCAT(z.id) as ids, GROUP_CONCAT(z.abotyp_name) as names from ZusatzAbo z GROUP BY z.haupt_abo_id) pairs
ON a.id = pairs.haupt_abo_id
SET a.zusatz_abo_ids = pairs.ids, a.zusatz_abotyp_names = pairs.names;""".execute.apply()

      sql"""update PostlieferungAbo a join
(SELECT haupt_abo_id, GROUP_CONCAT(z.id) as ids, GROUP_CONCAT(z.abotyp_name) as names from ZusatzAbo z GROUP BY z.haupt_abo_id) pairs
ON a.id = pairs.haupt_abo_id
SET a.zusatz_abo_ids = pairs.ids, a.zusatz_abotyp_names = pairs.names;""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(addZusatzAboInfoToAbo)
}
