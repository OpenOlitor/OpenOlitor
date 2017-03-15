package ch.openolitor.core.repositories

import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.eventsourcing.PersistenceDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder

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
}