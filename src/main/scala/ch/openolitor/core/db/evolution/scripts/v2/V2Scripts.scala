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
import ch.openolitor.stammdaten.StammdatenDBMappings

object V2Scripts {

  def oo656(sys: ActorSystem) = new Script with LazyLogging with CoreDBMappings with DefaultDBScripts with CoreRepositoryQueries {
    lazy val system: ActorSystem = sys

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
      val persistentActorStates = queryLatestPersistenceMessageByPersistenceIdQuery.apply() map { messagePerPersistenceId =>
        //find latest sequence nr
        logger.debug(s"OO-656: latest persistence id of persistentactor:${messagePerPersistenceId.persistenceId}, sequenceNr:${messagePerPersistenceId.sequenceNr}")
        PersistenceEventState(PersistenceEventStateId(), messagePerPersistenceId.persistenceId, messagePerPersistenceId.sequenceNr, 0L, DateTime.now, Boot.systemPersonId, DateTime.now, Boot.systemPersonId)
      }

      // append persistent views
      val persistentViewStates = persistentActorStates filter (_.persistenceId == "entity-store") flatMap (newState =>
        Seq("buchhaltung", "stammdaten") map { module =>
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

  val oo688 = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      sql"""create table ${zusatzAbotypMapping.table} (
        id BIGINT not null,
        name varchar(50) not null,
        beschreibung varchar(256),
        aktiv_von datetime default null,
        aktiv_bis datetime default null,
        preis DECIMAL(7,2) not null,
        preiseinheit varchar(20) not null,
        laufzeit int,
        laufzeiteinheit varchar(50),
        vertragslaufzeit varchar(50),
        kuendigungsfrist varchar(50),
        anzahl_abwesenheiten int, farb_code varchar(20),
        zielpreis DECIMAL(7,2),
        guthaben_mindestbestand int,
        admin_prozente DECIMAL(5,2),
        wird_geplant varchar(1) not null,
        anzahl_abonnenten INT not null,
        letzte_lieferung datetime default null,
        waehrung varchar(10),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${zusatzAboMapping.table}  (
        id BIGINT not null,
        haupt_abo_id BIGINT not null,
        haupt_abotyp_id BIGINT not null,
        kunde_id BIGINT not null,
        kunde varchar(100),
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        vertrieb_beschrieb varchar(2000),
        abotyp_id BIGINT not null,
        abotyp_name varchar(50),
        start datetime not null,
        ende datetime,
        guthaben_vertraglich int,
        guthaben int not null default 0,
        guthaben_in_rechnung int not null default 0,
        letzte_lieferung datetime,
        anzahl_abwesenheiten varchar(500),
        anzahl_lieferungen varchar(500),
        aktiv varchar(1),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      Success(true)
    }
  }

  def scripts(system: ActorSystem) = Seq(oo656(system), oo688) ++
    OO686_Add_Rechnungspositionen.scripts
}