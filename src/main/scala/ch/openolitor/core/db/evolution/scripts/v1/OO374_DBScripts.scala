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

object OO374_DBScripts extends DefaultDBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"add column anzahl_abos_aktiv to abotyp")
      alterTableAddColumnIfNotExists(abotypMapping, "anzahl_abonnenten_aktiv", "int not null default 0", "anzahl_abonnenten")
      logger.debug(s"add column anzahl_abos_aktiv to kunde")
      alterTableAddColumnIfNotExists(kundeMapping, "anzahl_abos_aktiv", "int not null default 0", "anzahl_abos")
      logger.debug(s"add column anzahl_abos_aktiv to vertrieb")
      alterTableAddColumnIfNotExists(vertriebMapping, "anzahl_abos_aktiv", "int not null default 0", "anzahl_lieferungen")
      logger.debug(s"add column anzahl_abos_aktiv to depotlieferung")
      alterTableAddColumnIfNotExists(depotlieferungMapping, "anzahl_abos_aktiv", "int not null default 0", "anzahl_abos")
      logger.debug(s"add column anzahl_abos_aktiv to heimlieferung")
      alterTableAddColumnIfNotExists(heimlieferungMapping, "anzahl_abos_aktiv", "int not null default 0", "anzahl_abos")
      logger.debug(s"add column anzahl_abos_aktiv to postlieferung")
      alterTableAddColumnIfNotExists(postlieferungMapping, "anzahl_abos_aktiv", "int not null default 0", "anzahl_abos")
      logger.debug(s"add column anzahl_abos_aktiv to depot")
      alterTableAddColumnIfNotExists(depotMapping, "anzahl_abonnenten_aktiv", "int not null default 0", "anzahl_abonnenten")
      logger.debug(s"add column anzahl_abos_aktiv to tour")
      alterTableAddColumnIfNotExists(tourMapping, "anzahl_abonnenten_aktiv", "int not null default 0", "anzahl_abonnenten")

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}