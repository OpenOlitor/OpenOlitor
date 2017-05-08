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

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.db.evolution.scripts.recalculations.RecalulateAnzahlAktiveAbosCounter
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

object OO597_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""ALTER TABLE ${bestellpositionMapping.table} MODIFY COLUMN bestellung_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX bestellung_id_index ON ${bestellpositionMapping.table} (bestellung_id)""".execute.apply()
      sql"""ALTER TABLE ${bestellpositionMapping.table} MODIFY COLUMN produkt_id BIGINT(20) DEFAULT NULL""".execute.apply()
      sql"""CREATE INDEX produkt_id_index ON ${bestellpositionMapping.table} (produkt_id)""".execute.apply()

      sql"""ALTER TABLE ${korbMapping.table} MODIFY COLUMN lieferung_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX lieferung_id_index ON ${korbMapping.table} (lieferung_id)""".execute.apply()
      sql"""ALTER TABLE ${korbMapping.table} MODIFY COLUMN abo_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX abo_id_index ON ${korbMapping.table} (abo_id)""".execute.apply()
      sql"""CREATE INDEX status_index ON ${korbMapping.table} (status)""".execute.apply()

      sql"""ALTER TABLE ${lieferpositionMapping.table} MODIFY COLUMN lieferung_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX lieferung_id_index ON ${lieferpositionMapping.table} (lieferung_id)""".execute.apply()
      sql"""ALTER TABLE ${lieferpositionMapping.table} MODIFY COLUMN produkt_id BIGINT(20) DEFAULT NULL""".execute.apply()
      sql"""CREATE INDEX produkt_id_index ON ${lieferpositionMapping.table} (produkt_id)""".execute.apply()
      sql"""ALTER TABLE ${lieferpositionMapping.table} MODIFY COLUMN produzent_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX produzent_id_index ON ${lieferpositionMapping.table} (produzent_id)""".execute.apply()

      sql"""ALTER TABLE ${lieferungMapping.table} MODIFY COLUMN abotyp_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${lieferungMapping.table} (abotyp_id)""".execute.apply()
      sql"""ALTER TABLE ${lieferungMapping.table} MODIFY COLUMN vertrieb_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${lieferungMapping.table} (vertrieb_id)""".execute.apply()
      sql"""ALTER TABLE ${lieferungMapping.table} MODIFY COLUMN lieferplanung_id BIGINT(20) DEFAULT NULL""".execute.apply()
      sql"""CREATE INDEX lieferplanung_id_index ON ${lieferungMapping.table} (lieferplanung_id)""".execute.apply()

      sql"""ALTER TABLE ${pendenzMapping.table} MODIFY COLUMN kunde_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${pendenzMapping.table} (kunde_id)""".execute.apply()

      sql"""ALTER TABLE ${personMapping.table} MODIFY COLUMN kunde_id BIGINT(20) NOT NULL""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${personMapping.table} (kunde_id)""".execute.apply()

      sql"""CREATE INDEX file_store_id_index ON ${projektVorlageMapping.table} (file_store_id)""".execute.apply()

      Success(true)
    }
  }

  val BuchhaltungScripts = new Script with LazyLogging with BuchhaltungDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""CREATE INDEX file_store_id_index ON ${rechnungMapping.table} (file_store_id)""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts, BuchhaltungScripts)
}
