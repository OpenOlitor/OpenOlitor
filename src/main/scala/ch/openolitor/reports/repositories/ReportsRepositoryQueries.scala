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
package ch.openolitor.reports.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseWriteRepository
import scala.concurrent._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.EventStream
import ch.openolitor.reports.models._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.reports.ReportsDBMappings

trait ReportsRepositoryQueries extends LazyLogging with ReportsDBMappings with StammdatenDBMappings {
  lazy val report = reportMapping.syntax("report")

  protected def getReportsQuery(filter: Option[FilterExpr]) = {
    val query = withSQL {
      select
        .from(reportMapping as report)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, report))
    }
    query.map(reportMapping(report)).list
  }

  protected def getReportQuery(id: ReportId) = {
    withSQL {
      select
        .from(reportMapping as report)
        .where.eq(report.id, id)
    }.map(reportMapping(report)).single
  }

  protected def executeReportQuery(reportExecute: ReportExecute) = {
    val query = SQL(reportExecute.query)
    query.map(rs => toMap(rs)).list
  }

  def toMap(rs: WrappedResultSet): Map[String, Any] = {
    (1 to rs.underlying.getMetaData.getColumnCount).foldLeft(Map[String, Any]()) { (result, i) =>
      val label = rs.underlying.getMetaData.getColumnLabel(i)
      Some(rs.any(label)).map { nullableValue => result + (label -> nullableValue) }.getOrElse(result)
    }
  }

}
