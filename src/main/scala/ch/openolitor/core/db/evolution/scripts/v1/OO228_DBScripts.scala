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

object OO228_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"drop table if exists ${tourlieferungMapping.table}".execute.apply()

      sql"drop table if exists ${depotAuslieferungMapping.table}".execute.apply()
      sql"drop table if exists ${tourAuslieferungMapping.table}".execute.apply()
      sql"drop table if exists ${postAuslieferungMapping.table}".execute.apply()

      sql"""create table ${tourlieferungMapping.table}  (
        id BIGINT not null,
        tour_id BIGINT not null,
        abotyp_id BIGINT not null,
        kunde_id BIGINT not null,
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        kunde_bezeichnung varchar(100),
        strasse varchar(50) not null,
        haus_nummer varchar(10),
        adress_zusatz varchar(100),
        plz varchar(10) not null,
        ort varchar(50) not null,
        abotyp_name varchar(50),
        sort INT,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${depotAuslieferungMapping.table}  (
        id BIGINT not null,
        lieferung_id BIGINT not null,
        status varchar(50) not null,
        depot_name varchar(50) not null,
        datum datetime not null,
        anzahl_koerbe INT not null, 
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${tourAuslieferungMapping.table}  (
        id BIGINT not null,
        lieferung_id BIGINT not null,
        status varchar(50) not null,
        tour_name varchar(50) not null,
        datum datetime not null,
        anzahl_koerbe INT not null, 
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${postAuslieferungMapping.table}  (
        id BIGINT not null,
        lieferung_id BIGINT not null,
        status varchar(50) not null,
        datum datetime not null,
        anzahl_koerbe INT not null, 
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      alterTableAddColumnIfNotExists(korbMapping, "auslieferung_id", "BIGINT", "guthaben_vor_lieferung")

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}