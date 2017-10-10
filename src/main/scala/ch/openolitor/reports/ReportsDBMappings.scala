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
package ch.openolitor.reports

import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.reports.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.core.repositories.DBMappings

//DB Model bindig
trait ReportsDBMappings extends DBMappings {
  import TypeBinder._

  // DB type binders for read operations
  implicit val reportsIdBinder: Binders[ReportId] = baseIdBinders(ReportId.apply _)
  //DB parameter binders for write and query operations
  //implicit val reportsIdSqlBinder: Binders[ReportId] = baseIdBinders(ReportId.apply _)

  implicit val reportMapping = new BaseEntitySQLSyntaxSupport[Report] {
    override val tableName = "Report"

    override lazy val columns = autoColumns[Report]()

    def apply(rn: ResultName[Report])(rs: WrappedResultSet): Report =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Report): Seq[ParameterBinder] =
      parameters(Report.unapply(entity).get)

    override def updateParameters(entity: Report) = {
      super.updateParameters(entity) ++ Seq(
        column.name -> entity.name,
        column.beschreibung -> entity.beschreibung,
        column.query -> entity.query
      )
    }
  }
}
