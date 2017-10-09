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

object OO554_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //update Abotyp.anzahl_abonnenten_aktiv
      sql"""UPDATE Abotyp SET anzahl_abonnenten_aktiv=0""".execute.apply()
      sql"""UPDATE Abotyp a
        LEFT JOIN
        (
        	SELECT abotyp_id, COUNT(*) c
        	FROM DepotlieferungAbo
        	WHERE aktiv=1
        	GROUP BY abotyp_id
        ) d ON a.id = d.abotyp_id
        LEFT JOIN
        (
        	SELECT abotyp_id, COUNT(*) c
        	FROM HeimlieferungAbo
        	WHERE aktiv=1
        	GROUP BY abotyp_id
        ) h ON a.id = h.abotyp_id
        LEFT JOIN
        (
        	SELECT abotyp_id, COUNT(*) c
        	FROM PostlieferungAbo
        	WHERE aktiv=1
        	GROUP BY abotyp_id
        ) p ON a.id = p.abotyp_id
        SET a.anzahl_abonnenten_aktiv = (IFNULL(d.c, 0) + IFNULL(h.c, 0) + IFNULL(p.c, 0))""".execute.apply()

      //update Kunde.anzahl_abos_aktiv
      sql"""UPDATE Kunde SET anzahl_abos_aktiv=0""".execute.apply()
      sql"""UPDATE Kunde k
        LEFT JOIN
        (
        	SELECT kunde_id, COUNT(*) c
        	FROM DepotlieferungAbo
        	WHERE aktiv=1
        	GROUP BY kunde_id
        ) d ON k.id = d.kunde_id
        LEFT JOIN
        (
        	SELECT kunde_id, COUNT(*) c
        	FROM HeimlieferungAbo
        	WHERE aktiv=1
        	GROUP BY kunde_id
        ) h ON k.id = h.kunde_id
        LEFT JOIN
        (
        	SELECT kunde_id, COUNT(*) c
        	FROM PostlieferungAbo
        	WHERE aktiv=1
        	GROUP BY kunde_id
        ) p ON k.id = p.kunde_id
        SET k.anzahl_abos_aktiv = (IFNULL(d.c, 0) + IFNULL(h.c, 0) + IFNULL(p.c, 0))""".execute.apply()

      //update Vertrieb.anzahl_abos_aktiv
      sql"""UPDATE Vertrieb SET anzahl_abos_aktiv=0""".execute.apply()
      sql"""UPDATE Vertrieb v
        LEFT JOIN
        (
        	SELECT vertrieb_id, COUNT(*) c
        	FROM DepotlieferungAbo
        	WHERE aktiv=1
        	GROUP BY vertrieb_id
        ) d ON v.id = d.vertrieb_id
        LEFT JOIN
        (
        	SELECT vertrieb_id, COUNT(*) c
        	FROM HeimlieferungAbo
        	WHERE aktiv=1
        	GROUP BY vertrieb_id
        ) h ON v.id = h.vertrieb_id
        LEFT JOIN
        (
        	SELECT vertrieb_id, COUNT(*) c
        	FROM PostlieferungAbo
        	WHERE aktiv=1
        	GROUP BY vertrieb_id
        ) p ON v.id = p.vertrieb_id
        SET v.anzahl_abos_aktiv = (IFNULL(d.c, 0) + IFNULL(h.c, 0) + IFNULL(p.c, 0))""".execute.apply()

      //update  Depotlieferung.anzahl_abos_aktiv
      sql"""UPDATE Depotlieferung SET anzahl_abos_aktiv=0""".execute.apply()
      sql"""UPDATE Depotlieferung d
        INNER JOIN
        (
        	SELECT depot_id, vertrieb_id, COUNT(*) c
        	FROM DepotlieferungAbo
        	WHERE aktiv=1
        	GROUP BY depot_id, vertrieb_id
        ) j ON d.vertrieb_id = j.vertrieb_id AND d.depot_id = j.depot_id
        SET d.anzahl_abos_aktiv = j.c""".execute.apply()

      //update  Heimlieferung.anzahl_abos_aktiv
      sql"""UPDATE Heimlieferung SET anzahl_abos_aktiv=0""".execute.apply()
      sql"""UPDATE Heimlieferung h
        INNER JOIN
        (
        	SELECT vertrieb_id, COUNT(*) c
        	FROM HeimlieferungAbo
        	WHERE aktiv=1
        	GROUP BY vertrieb_id
        ) j ON h.vertrieb_id = j.vertrieb_id
        SET h.anzahl_abos_aktiv = j.c""".execute.apply()

      //update  Postlieferung.anzahl_abos_aktiv
      sql"""UPDATE Postlieferung SET anzahl_abos_aktiv=0""".execute.apply()
      sql"""UPDATE Postlieferung p
        INNER JOIN
        (
        	SELECT vertrieb_id, COUNT(*) c
        	FROM PostlieferungAbo
        	WHERE aktiv=1
        	GROUP BY vertrieb_id
        ) j ON p.vertrieb_id = j.vertrieb_id
        SET p.anzahl_abos_aktiv = j.c""".execute.apply()

      //update  Depot.anzahl_abonnenten_aktiv
      sql"""UPDATE Depot SET anzahl_abonnenten_aktiv=0""".execute.apply()
      sql"""UPDATE Depot d
        INNER JOIN
        (
        	SELECT depot_id, COUNT(*) c
        	FROM DepotlieferungAbo
        	GROUP BY depot_id
        ) j ON d.id = j.depot_id
        SET d.anzahl_abonnenten_aktiv = j.c""".execute.apply()

      //update  Tour.anzahl_abonnenten_aktiv
      sql"""UPDATE Tour SET anzahl_abonnenten_aktiv=0""".execute.apply()
      sql"""UPDATE Tour t
        INNER JOIN
        (
        	SELECT tour_id, COUNT(*) c
        	FROM Tourlieferung
        	GROUP BY tour_id
        ) j ON t.id = j.tour_id
        SET t.anzahl_abonnenten_aktiv = j.c""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}
