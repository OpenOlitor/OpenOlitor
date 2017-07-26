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
import ch.openolitor.stammdaten.models._
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

/**
 * Remaining indexes from OO597_DBScripts
 */
object OO597_Index_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""CREATE INDEX id_index ON ${abotypMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${abwesenheitMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX abo_id_index ON ${abwesenheitMapping.table} (abo_id)""".execute.apply()
      sql"""CREATE INDEX lieferung_id_index ON ${abwesenheitMapping.table} (lieferung_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${bestellpositionMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${bestellungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX sammelbestellung_id_index ON ${bestellungMapping.table} (sammelbestellung_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${depotMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${depotAuslieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX depot_id_index ON ${depotAuslieferungMapping.table} (depot_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${depotlieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${depotlieferungMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX depot_id_index ON ${depotlieferungMapping.table} (depot_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${depotlieferungAboMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${depotlieferungAboMapping.table} (kunde_id)""".execute.apply()
      sql"""CREATE INDEX vertriebsart_id_index ON ${depotlieferungAboMapping.table} (vertriebsart_id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${depotlieferungAboMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${depotlieferungAboMapping.table} (abotyp_id)""".execute.apply()
      sql"""CREATE INDEX depot_id_index ON ${depotlieferungAboMapping.table} (depot_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${einladungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX person_id_index ON ${einladungMapping.table} (person_id)""".execute.apply()
      sql"""CREATE INDEX uid_index ON ${einladungMapping.table} (uid)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${heimlieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${heimlieferungMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX tour_id_index ON ${heimlieferungMapping.table} (tour_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${heimlieferungAboMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${heimlieferungAboMapping.table} (kunde_id)""".execute.apply()
      sql"""CREATE INDEX vertriebsart_id_index ON ${heimlieferungAboMapping.table} (vertriebsart_id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${heimlieferungAboMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${heimlieferungAboMapping.table} (abotyp_id)""".execute.apply()
      sql"""CREATE INDEX tour_id_index ON ${heimlieferungAboMapping.table} (tour_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${korbMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX auslieferung_id_index ON ${korbMapping.table} (auslieferung_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${customKundentypMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${lieferplanungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${lieferpositionMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${lieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${pendenzMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${personMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${postAuslieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${postlieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${postlieferungMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${postlieferungAboMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${postlieferungAboMapping.table} (kunde_id)""".execute.apply()
      sql"""CREATE INDEX vertriebsart_id_index ON ${postlieferungAboMapping.table} (vertriebsart_id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${postlieferungAboMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${postlieferungAboMapping.table} (abotyp_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${produktMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${produktProduktekategorieMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX produkt_id_index ON ${produktProduktekategorieMapping.table} (produkt_id)""".execute.apply()
      sql"""CREATE INDEX produktekategorie_id_index ON ${produktProduktekategorieMapping.table} (produktekategorie_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${produktProduzentMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX produkt_id_index ON ${produktProduzentMapping.table} (produkt_id)""".execute.apply()
      sql"""CREATE INDEX produzent_id_index ON ${produktProduzentMapping.table} (produzent_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${produktekategorieMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${produzentMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${projektMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${projektVorlageMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${sammelbestellungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX produzent_id_index ON ${sammelbestellungMapping.table} (produzent_id)""".execute.apply()
      sql"""CREATE INDEX lieferplanung_id_index ON ${sammelbestellungMapping.table} (lieferplanung_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${tourMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${tourAuslieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX tour_id_index ON ${tourAuslieferungMapping.table} (tour_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${tourlieferungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX tour_id_index ON ${tourlieferungMapping.table} (tour_id)""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${tourlieferungMapping.table} (abotyp_id)""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${tourlieferungMapping.table} (kunde_id)""".execute.apply()
      sql"""CREATE INDEX vertriebsart_id_index ON ${tourlieferungMapping.table} (vertriebsart_id)""".execute.apply()
      sql"""CREATE INDEX vertrieb_id_index ON ${tourlieferungMapping.table} (vertrieb_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${vertriebMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX abotyp_id_index ON ${vertriebMapping.table} (abotyp_id)""".execute.apply()

      Success(true)
    }
  }

  val BuchhaltungScripts = new Script with LazyLogging with BuchhaltungDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""CREATE INDEX id_index ON ${rechnungMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX kunde_id_index ON ${rechnungMapping.table} (kunde_id)""".execute.apply()
      sql"""CREATE INDEX abo_id_index ON ${rechnungMapping.table} (abo_id)""".execute.apply()

      sql"""CREATE INDEX id_index ON ${zahlungsEingangMapping.table} (id)""".execute.apply()
      sql"""CREATE INDEX zahlungs_import_id_index ON ${zahlungsEingangMapping.table} (zahlungs_import_id)""".execute.apply()
      sql"""CREATE INDEX rechnung_id_index ON ${zahlungsEingangMapping.table} (rechnung_id)""".execute.apply()
      sql"""CREATE INDEX id_index ON ${zahlungsImportMapping.table} (id)""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts, BuchhaltungScripts)
}
