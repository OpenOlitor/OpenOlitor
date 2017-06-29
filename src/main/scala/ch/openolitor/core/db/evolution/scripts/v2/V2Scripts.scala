package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.db.evolution._
import ch.openolitor.core.repositories.CoreDBMappings
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scalikejdbc.SQLSyntax._
import scala.util.{ Try, Success }
import ch.openolitor.util.IdUtil
import org.joda.time.DateTime
import ch.openolitor.core.Boot

object V2Scripts {  

  val oo656 = new Script with LazyLogging with CoreDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"creating PersistenceEventState")

      sql"""create table ${persistenceEventStateMapping.table}  (
        id BIGINT not null,
        persistence_id varchar(100) not null,
        last_sequence_nr BIGINT default (0),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      logger.debug(s"store last sequence number for persistent actors")
      for { module <- Seq("buchhaltung", "stammdaten") } {
        sql"""insert into ${persistenceEventStateMapping.table} 
          (id, $module-entity-store, last_sequence_nr, erstelltdat, ersteller, modifidat, modifikator)
          SELECT 
          ${IdUtil.positiveRandomId}, persistence_key, max(sequence_nr), ${parameter(DateTime.now)}, ${parameter(Boot.systemPersonId)}, ${parameter(DateTime.now)}, ${parameter(Boot.systemPersonId)}
          FROM 
          persistence_journal group by persistence_key where persistence_key='$module'""".execute.apply()
      }

      logger.debug(s"store last sequence number for persistent views")

      Success(true)
    }
  }
  
  val scripts = Seq(oo656)
}