package ch.openolitor.core.repositories

import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.util.parsing.FilterExpr
import scala.concurrent.Future
import ch.openolitor.core.models.PersistenceJournal
import akka.actor.ActorSystem

trait CoreReadRepository {
  def queryPersistenceJournal(limit: Int)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersistenceJournal]]
}

class CoreReadRepositoryImpl(override val system: ActorSystem) extends CoreReadRepository with CoreRepositoryQueries {
  def queryPersistenceJournal(limit: Int)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersistenceJournal]] =
    queryPersistenceJournalQuery(limit, filter).future
}