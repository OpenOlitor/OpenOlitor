package ch.openolitor.core.repositories

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.util.IdUtil
import ch.openolitor.core.JSONSerializable
import ch.openolitor.util.IdUtil
import ch.openolitor.stammdaten.models.ProjektReport
import ch.openolitor.core.reporting.models._

trait ReportReadRepository {
  def getMultiReport[T <% JSONSerializable](projekt: ProjektReport, source: Future[List[T]])(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[MultiReport[T]] = {
    source map { entries =>
      MultiReport(MultiReportId(IdUtil.positiveRandomId), entries, projekt)
    }
  }
}

