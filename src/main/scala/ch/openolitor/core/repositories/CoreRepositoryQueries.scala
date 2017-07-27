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
package ch.openolitor.core.repositories

import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.eventsourcing.PersistenceDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.core.models.PersistenceMessage

trait CoreRepositoryQueries extends LazyLogging with CoreDBMappings with PersistenceDBMappings {
  lazy val persistenceJournal = persistenceJournalMapping.syntax("persistence")
  lazy val persistenceMeta = persistenceMetadataMapping.syntax("persistenceMeta")

  protected def queryPersistenceJournalQuery(limit: Int, filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(persistenceJournalMapping as persistenceJournal)
        .innerJoin(persistenceMetadataMapping as persistenceMeta).on(persistenceJournal.persistenceKey, persistenceMeta.persistenceKey)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, persistenceJournal))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, persistenceMeta, Seq("sequence_nr")))
        .orderBy(persistenceJournal.sequenceNr.desc)
        .limit(limit)
    }.map(persistenceJournalMapping(persistenceJournal)).list
  }

  protected def queryLatestPersistenceMessageByPersistenceIdQuery = {
    sql"""SELECT l.persistence_id, l.persistence_key, l.sequence_nr, j.message FROM
      persistence_journal j INNER JOIN (
        SELECT j.persistence_key, m.persistence_id, max(j.sequence_nr) sequence_nr
          FROM persistence_journal j JOIN persistence_metadata m ON j.persistence_key=m.persistence_key group by j.persistence_key, m.persistence_id) l
        ON j.persistence_key=l.persistence_key AND j.sequence_nr=l.sequence_nr
          """.map { rs =>
      val persistenceId = rs.string("persistence_id")
      val persistenceKey = rs.long("persistence_key")
      val seqNr = rs.long("sequence_nr")
      val message = persistentEventBinder.apply(rs.underlying, "message")
      logger.debug(s"Get latest message per persistenceId:$persistenceId, sequenceNr: $seqNr, message:$message")
      PersistenceMessage(persistenceId, seqNr, message)
    }.list
  }
}