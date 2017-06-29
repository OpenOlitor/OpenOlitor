package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.db.evolution._
import ch.openolitor.core.repositories.CoreDBMappings
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scalikejdbc.SQLSyntax._
import scala.util.{ Try, Success }

object V2Scripts {
  val scripts = Seq(oo656)

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

      // TODO: update to latest sequence nr read from journal        

      Success(true)
    }
  }
}