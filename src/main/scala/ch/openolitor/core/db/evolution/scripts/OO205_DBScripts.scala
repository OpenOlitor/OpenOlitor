package ch.openolitor.core.db.evolution.scripts

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import scala.util.{ Try, Success }

object OO205_DBScripts {

  val StammdatenDBScript = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"alter table ${lieferplanungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${lieferungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${lieferpositionMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${bestellungMapping.table} modify Column id BIGINT".execute.apply()
      sql"alter table ${bestellpositionMapping.table}  modify Column id BIGINT".execute.apply()
      sql"alter table ${korbMapping.table} modify Column id BIGINT".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenDBScript)
}