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
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

/**
 * Remaining indexes from OO597_DBScripts
 */
object OO686_Add_Rechnungspositionen {
  val CreateRechnungsPositionen = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
CREATE TABLE `RechnungsPosition` (
  `id` bigint(20) NOT NULL,
  `rechnung_id` bigint(20),
  `parent_rechnungs_position_id` bigint(20),
  `abo_id` bigint(20),
  `kunde_id` bigint(20) NOT NULL,
  `betrag` decimal(8,2) NOT NULL,
  `waehrung` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `anzahl_lieferungen` int(11),
  `beschrieb` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `typ` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `sort` int(11),
  `erstelldat` datetime NOT NULL,
  `ersteller` bigint(20) NOT NULL,
  `modifidat` datetime NOT NULL,
  `modifikator` bigint(20) NOT NULL,
  KEY `id_index` (`id`),
  KEY `rechnung_id_index` (`rechnung_id`),
  KEY `parent_rechnungs_position_id_index` (`parent_rechnungs_position_id`),
  KEY `abo_id_index` (`abo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci
""".execute.apply()
      Success(true)
    }
  }

  val FillRechnungsPositionenFromRechnungen = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
INSERT INTO RechnungsPosition 
  (id, rechnung_id, abo_id, kunde_id, betrag, waehrung, anzahl_lieferungen, beschrieb, 
   status, typ, sort, erstelldat, ersteller, modifidat, modifikator)
SELECT 
   id, id, abo_id, kunde_id, betrag, waehrung, anzahl_lieferungen, titel, 
   CASE WHEN status = 'Bezahlt' then 'Bezahlt' else 'Zugewiesen' END AS status, 'Abo', 1, erstelldat, ersteller, modifidat, modifikator FROM Rechnung
""".execute.apply()
      Success(true)
    }
  }

  val DropColumnsFromRechnungen = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""
ALTER TABLE Rechnung DROP COLUMN abo_id, DROP COLUMN anzahl_lieferungen
""".execute.apply()
      Success(true)
    }
  }

  val scripts = Seq(CreateRechnungsPositionen, FillRechnungsPositionenFromRechnungen, DropColumnsFromRechnungen)
}
