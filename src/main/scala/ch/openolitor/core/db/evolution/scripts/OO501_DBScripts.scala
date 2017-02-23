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

object OO501_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      // create table sammelbestellung
      logger.debug(s"creating sammelbestellung")

      sql"""create table ${sammelbestellungMapping.table}  (
        id BIGINT not null,
        produzent_id BIGINT not null,
        produzent_kurzzeichen varchar(6) not null,
        lieferplanung_id BIGINT not null,
        status varchar(50) not null,
        datum datetime not null,
        datum_abrechnung datetime default null,
        datum_versendet datetime,
        preis_total DECIMAL(7,2) not null,
        steuer_satz DECIMAL(4,2),
        steuer DECIMAL(7,2) not null,
        total_steuer DECIMAL(7,2) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      alterTableAddColumnIfNotExists(bestellungMapping, "sammelbestellung_id", "BIGINT not null default 0", "id")

      alterTableAddColumnIfNotExists(bestellungMapping, "admin_prozente", "DECIMAL(4,2) not null default 0", "total_steuer")

      alterTableAddColumnIfNotExists(bestellungMapping, "admin_prozente_abzug", "DECIMAL(7,2) not null default 0", "admin_prozente")

      alterTableAddColumnIfNotExists(bestellungMapping, "total_nach_abzug_admin_prozente", "DECIMAL(7,2) not null default 0", "admin_prozente_abzug")

      // update admin_prozente
      sql"""update ${bestellungMapping.table} u 
          inner join (select lp.id as lieferplanung_id, max(a.admin_prozente) as admin_prozente from ${lieferplanungMapping.table} lp 
            inner join ${lieferungMapping.table} l on (lp.id = l.lieferplanung_id)
            inner join ${abotypMapping.table} a on (l.abotyp_id = a.id)
            group by lieferplanung_id) sub
          on (sub.lieferplanung_id = u.lieferplanung_id)
          set u.admin_prozente = sub.admin_prozente""".execute.apply()

      // calculate total_nach_abzug_admin_prozente
      sql"""update ${bestellungMapping.table} 
        set total_nach_abzug_admin_prozente = preis_total - (preis_total * admin_prozente / 100), admin_prozente_abzug = preis_total * admin_prozente / 100""".execute.apply()

      // re-calculate total_steuer
      sql"""update ${bestellungMapping.table} 
        set total_steuer = total_nach_abzug_admin_prozente + (coalesce(steuer_satz, 0) / 100 * total_nach_abzug_admin_prozente), steuer = coalesce(steuer_satz, 0) / 100 * total_nach_abzug_admin_prozente""".execute.apply()

      // set sammelbestellung_id to self.id
      sql"""update ${bestellungMapping.table} 
        set sammelbestellung_id = id""".execute.apply()

      // this migration creates a simple 1-1 mapping from bestellung to sammelbestellung
      sql"""insert into ${sammelbestellungMapping.table} 
          (id, produzent_id, produzent_kurzzeichen, lieferplanung_id, status, datum, datum_abrechnung, datum_versendet, preis_total, steuer_satz, steuer, total_steuer, erstelldat, ersteller, modifidat, modifikator)
          select 
          id, produzent_id, produzent_kurzzeichen, lieferplanung_id, status, datum, datum_abrechnung, datum_versendet, preis_total, steuer_satz, steuer, total_steuer, erstelldat, ersteller, modifidat, modifikator 
          from ${bestellungMapping.table}""".execute.apply()

      // remove obsolete columns of bestellung
      sql"""alter table ${bestellungMapping.table} drop column produzent_id""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column produzent_kurzzeichen""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column lieferplanung_id""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column status""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column datum""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column datum_abrechnung""".execute.apply()

      sql"""alter table ${bestellungMapping.table} drop column datum_versendet""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}
