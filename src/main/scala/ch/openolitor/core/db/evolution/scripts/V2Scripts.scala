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
      logger.debug(s"Add column sprache to projekt...")
      // add column sprache to projekt
      sql"ALTER TABLE ${projektMapping.table} ADD COLUMN IF NOT EXISTS sprache varchar(10) ".execute.apply()
      Success(true)
    }
  }

  val scripts = V1Scripts.scripts ++ Seq(StammdatenScripts)
}