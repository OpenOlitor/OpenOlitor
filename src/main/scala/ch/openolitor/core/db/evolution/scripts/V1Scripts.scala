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

import ch.openolitor.core.db.evolution._
import scalikejdbc._
import scala.util._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings

object V1Scripts {
  val scripts = Seq(
      DBInitializationScript      
  )
  
  val DBInitializationScript = new Script with LazyLogging with StammdatenDBMappings {
    def execute(implicit session: DBSession): Try[Boolean] = {
          //drop all tables
      logger.debug(s"oo-system: cleanupDatabase - drop tables")

      sql"drop table if exists ${postlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotMapping.table}".execute.apply()
      sql"drop table if exists ${tourMapping.table}".execute.apply()
      sql"drop table if exists ${abotypMapping.table}".execute.apply()
      sql"drop table if exists ${kundeMapping.table}".execute.apply()
      sql"drop table if exists ${pendenzMapping.table}".execute.apply()
      sql"drop table if exists ${customKundentypMapping.table}".execute.apply()
      sql"drop table if exists ${personMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${postlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${lieferungMapping.table}".execute.apply()
      sql"drop table if exists ${produktMapping.table}".execute.apply()
      sql"drop table if exists ${produktekategorieMapping.table}".execute.apply()
      sql"drop table if exists ${produzentMapping.table}".execute.apply()
      sql"drop table if exists ${projektMapping.table}".execute.apply()
      sql"drop table if exists ${produktProduzentMapping.table}".execute.apply()
      sql"drop table if exists ${produktProduktekategorieMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables")
      //create tables

      sql"create table ${postlieferungMapping.table}  (id varchar(36) not null, abotyp_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${depotlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, depot_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${heimlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, tour_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${depotMapping.table} (id varchar(36) not null, name varchar(50) not null, kurzzeichen varchar(6) not null, ap_name varchar(50), ap_vorname varchar(50), ap_telefon varchar(20), ap_email varchar(100), v_name varchar(50), v_vorname varchar(50), v_telefon varchar(20), v_email varchar(100), strasse varchar(50), haus_nummer varchar(10), plz varchar(4) not null, ort varchar(50) not null, aktiv bit, oeffnungszeiten varchar(200), iban varchar(34), bank varchar(50), beschreibung varchar(200), anzahl_abonnenten_max int, anzahl_abonnenten int not null)".execute.apply()
      sql"create table ${tourMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${abotypMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), aktiv_von datetime default null, aktiv_bis datetime default null, preis DECIMAL(7,2) not null, preiseinheit varchar(20) not null, laufzeit int, laufzeiteinheit varchar(50), anzahl_abwesenheiten int, farb_code varchar(20), zielpreis DECIMAL(7,2), saldo_mindestbestand int, anzahl_abonnenten INT not null, letzte_lieferung datetime default null, waehrung varchar(10))".execute.apply()
      sql"create table ${kundeMapping.table} (id varchar(36) not null, bezeichnung varchar(50), strasse varchar(50) not null, haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4) not null, ort varchar(50) not null, bemerkungen varchar(512), strasse_lieferung varchar(50), haus_nummer_lieferung varchar(10), adress_zusatz_lieferung varchar(100), plz_lieferung varchar(4), ort_lieferung varchar(50), typen varchar(200), anzahl_abos int not null, anzahl_pendenzen int not null, anzahl_personen int not null)".execute.apply()
      sql"create table ${pendenzMapping.table} (id varchar(36) not null, kunde_id varchar(50) not null, kunde_bezeichnung varchar(50), datum datetime default null, bemerkung varchar(2000), status varchar(10))".execute.apply()
      sql"create table ${customKundentypMapping.table} (id varchar(36) not null, kundentyp varchar(50) not null, beschreibung varchar(250), anzahl_verknuepfungen int not null)".execute.apply()
      sql"create table ${personMapping.table} (id varchar(36) not null, kunde_id varchar(50) not null, name varchar(50) not null, vorname varchar(50) not null, email varchar(100) not null, email_alternative varchar(100), telefon_mobil varchar(50), telefon_festnetz varchar(50), bemerkungen varchar(512), sort int not null)".execute.apply()
      sql"create table ${depotlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), depot_id varchar(36), depot_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${heimlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), tour_id varchar(36), tour_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${postlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${lieferungMapping.table}  (id varchar(36) not null,abotyp_id varchar(36) not null, vertriebsart_id varchar(36) not null,datum datetime not null, anzahl_abwesenheiten int not null,status varchar(50) not null)".execute.apply()
      sql"create table ${produktMapping.table}  (id varchar(36) not null, name varchar(50) not null, verfuegbar_von varchar(10) not null, verfuegbar_bis varchar(10) not null, kategorien varchar(300), einheit varchar(20) not null, preis DECIMAL(7,2) not null, produzenten varchar(300))".execute.apply()
      sql"create table ${produktekategorieMapping.table}  (id varchar(36) not null, beschreibung varchar(50) not null)".execute.apply()
      sql"create table ${produzentMapping.table}  (id varchar(36) not null, name varchar(50) not null, vorname varchar(50), kurzzeichen varchar(6) not null, strasse varchar(50), haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4) not null, ort varchar(50) not null, bemerkungen varchar(1000), email varchar(100) not null, telefon_mobil varchar(50), telefon_festnetz varchar(50), iban varchar(34), bank varchar(50), mwst bit, mwst_satz DECIMAL(4,2), aktiv bit)".execute.apply()
      sql"create table ${projektMapping.table}  (id varchar(36) not null, bezeichnung varchar(50) not null, strasse varchar(50), haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4), ort varchar(50), waehrung varchar(10) not null)".execute.apply()
      sql"create table ${produktProduzentMapping.table} (id varchar(36) not null, produkt_id varchar(36) not null, produzent_id varchar(36) not null)".execute.apply()
      sql"create table ${produktProduktekategorieMapping.table} (id varchar(36) not null, produkt_id varchar(36) not null, produktekategorie_id varchar(36) not null)".execute.apply()
      
      logger.debug(s"oo-system: cleanupDatabase - end")
      Success(true)
    }
  }
}