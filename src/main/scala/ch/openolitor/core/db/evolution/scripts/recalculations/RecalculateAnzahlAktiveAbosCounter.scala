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
package ch.openolitor.core.db.evolution.scripts.recalculations

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts

object RecalulateAnzahlAktiveAbosCounter {
  val scripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""update ${depotMapping.table} u 
        INNER JOIN (SELECT a.depot_id, count(a.id) counter FROM ${depotlieferungAboMapping.table} a 
        WHERE a.aktiv=true group by a.depot_id) a ON u.id=a.depot_id 
        SET u.anzahl_abonnenten_aktiv=a.counter""".execute.apply()

      sql"""update ${tourMapping.table} u 
        INNER JOIN (SELECT a.tour_id, count(a.id) counter FROM ${heimlieferungAboMapping.table} a 
        WHERE a.aktiv=true group by a.tour_id) a ON u.id=a.tour_id 
        SET u.anzahl_abonnenten_aktiv=a.counter""".execute.apply()

      sql"""update ${depotlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${depotlieferungAboMapping.table} a 
        WHERE a.aktiv=true group by a.vertriebsart_id) a ON u.id=a.vertriebsart_id 
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${heimlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${heimlieferungAboMapping.table} a 
        WHERE a.aktiv=true group by a.vertriebsart_id) a ON u.id=a.vertriebsart_id 
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${postlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${postlieferungAboMapping.table} a 
        WHERE a.aktiv=true group by a.vertriebsart_id) a ON u.id=a.vertriebsart_id
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${abotypMapping.table} u 
        LEFT OUTER JOIN (SELECT d.abotyp_id, count(d.id) counter FROM ${depotlieferungAboMapping.table} d 
        WHERE d.aktiv=true group by d.abotyp_id) d ON u.id=d.abotyp_id 
        LEFT OUTER JOIN (SELECT t.abotyp_id, count(t.id) counter FROM ${heimlieferungAboMapping.table} t 
        WHERE t.aktiv=true group by t.abotyp_id) t ON u.id=t.abotyp_id 
        LEFT OUTER JOIN (SELECT p.abotyp_id, count(p.id) counter FROM ${postlieferungAboMapping.table} p 
        WHERE p.aktiv=true group by p.abotyp_id) p ON u.id=p.abotyp_id 
        SET u.anzahl_abonnenten_aktiv=COALESCE(d.counter, 0) + COALESCE(t.counter, 0) + COALESCE(p.counter, 0)""".execute.apply()

      sql"""update ${kundeMapping.table} u 
        LEFT OUTER JOIN (SELECT d.kunde_id, count(d.id) counter FROM ${depotlieferungAboMapping.table} d 
        WHERE d.aktiv=true group by d.kunde_id) d ON u.id=d.kunde_id 
        LEFT OUTER JOIN (SELECT t.kunde_id, count(t.id) counter FROM ${heimlieferungAboMapping.table} t 
        WHERE t.aktiv=true group by t.kunde_id) t ON u.id=t.kunde_id 
        LEFT OUTER JOIN (SELECT p.kunde_id, count(p.id) counter FROM ${postlieferungAboMapping.table} p 
        WHERE p.aktiv=true group by p.kunde_id) p ON u.id=p.kunde_id 
        SET u.anzahl_abos_aktiv=COALESCE(d.counter, 0) + COALESCE(t.counter, 0) + COALESCE(p.counter, 0)""".execute.apply()

      sql"""update ${vertriebMapping.table} u 
        LEFT OUTER JOIN (SELECT d.vertrieb_id, count(d.id) counter FROM ${depotlieferungAboMapping.table} d 
        WHERE d.aktiv=true group by d.vertrieb_id) d ON u.id=d.vertrieb_id 
        LEFT OUTER JOIN (SELECT t.vertrieb_id, count(t.id) counter FROM ${heimlieferungAboMapping.table} t 
        WHERE t.aktiv=true group by t.vertrieb_id) t ON u.id=t.vertrieb_id 
        LEFT OUTER JOIN (SELECT p.vertrieb_id, count(p.id) counter FROM ${postlieferungAboMapping.table} p 
        WHERE p.aktiv=true group by p.vertrieb_id) p ON u.id=p.vertrieb_id 
        SET u.anzahl_abos_aktiv=COALESCE(d.counter, 0) + COALESCE(t.counter, 0) + COALESCE(p.counter, 0)""".execute.apply()

      Success(true)
    }
  }
}