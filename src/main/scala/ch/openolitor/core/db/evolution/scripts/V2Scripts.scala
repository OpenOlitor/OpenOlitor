package ch.openolitor.core.db.evolution.scripts

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success

/**
 * This scripts contains changes after first release of 01.06.2016
 */
object V2Scripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
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

      sql"ALTER TABLE ${korbMapping.table} ADD COLUMN IF NOT EXISTS auslieferung_id BIGINT AFTER guthaben_vor_lieferung".execute.apply()

      logger.debug(s"Add column sprache to projekt...")
      // add column sprache to projekt
      sql"ALTER TABLE ${projektMapping.table} ADD COLUMN IF NOT EXISTS sprache varchar(10) ".execute.apply()
      Success(true)
    }
  }

  val scripts = V1Scripts.scripts ++ Seq(StammdatenScripts)
}