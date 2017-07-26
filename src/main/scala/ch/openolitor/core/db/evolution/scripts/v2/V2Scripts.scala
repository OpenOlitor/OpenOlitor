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
import ch.openolitor.core.repositories.CoreRepositoryQueries
import ch.openolitor.core.models.PersistenceEventState
import ch.openolitor.core.models.PersistenceEventStateId
import ch.openolitor.core.eventsourcing.EventStoreSerializer
import ch.openolitor.core.domain.PersistentEvent
import ch.openolitor.core.repositories.BaseWriteRepository
import akka.actor.ActorSystem

object V2Scripts {

  def oo656(sytem: ActorSystem) = new Script with LazyLogging with CoreDBMappings with DefaultDBScripts with CoreRepositoryQueries {
    lazy val system: ActorSystem = sytem

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"creating PersistenceEventState")

      sql"""drop table if exists ${persistenceEventStateMapping.table}""".execute.apply()

      sql"""create table ${persistenceEventStateMapping.table}  (
        id BIGINT not null,
        persistence_id varchar(100) not null,
        last_transaction_nr BIGINT default 0,
        last_sequence_nr BIGINT default 0,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      logger.debug(s"store last sequence number for actors and persistence views")
      val persistentActorStates = queryLatestPersistenceMessageByPersistenceIdQuery.apply().map { messagePerPersistenceId =>
        //find latest sequence nr
        logger.debug(s"OO-656: latest persistence id of persistentactor:${messagePerPersistenceId.persistenceId}, sequenceNr:${messagePerPersistenceId.sequenceNr}")
        PersistenceEventState(PersistenceEventStateId(), messagePerPersistenceId.persistenceId, messagePerPersistenceId.sequenceNr, 0L, DateTime.now, Boot.systemPersonId, DateTime.now, Boot.systemPersonId)
      }

      // append persistent views
      val persistentViewStates = persistentActorStates.filter(_.persistenceId == "entity-store").flatMap(newState =>
        Seq("buchhaltung", "stammdaten").map { module =>
          logger.debug(s"OO-656: latest persistence id of persistentview:$module-entity-store, sequenceNr:${newState.lastTransactionNr}")
          PersistenceEventState(PersistenceEventStateId(), s"$module-entity-store", newState.lastTransactionNr, 0L, DateTime.now, Boot.systemPersonId, DateTime.now, Boot.systemPersonId)
        })

      implicit val personId = Boot.systemPersonId
      (persistentActorStates ++ persistentViewStates) map { entity =>
        val params = persistenceEventStateMapping.parameterMappings(entity)
        withSQL(insertInto(persistenceEventStateMapping).values(params: _*)).update.apply()
      }

      // stop all entity-store snapshots due to class incompatiblity
      sql"""truncate persistence_snapshot""".execute.apply()

      Success(true)
    }
  }

  def scripts(system: ActorSystem) = Seq(oo656(system))
}