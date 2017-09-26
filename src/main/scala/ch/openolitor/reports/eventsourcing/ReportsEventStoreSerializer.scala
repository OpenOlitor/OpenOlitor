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
package ch.openolitor.reports.eventsourcing

import spray.json.DefaultJsonProtocol
import stamina._
import stamina.json._
import ch.openolitor.reports._
import ch.openolitor.reports.models._
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import zangelo.spray.json.AutoProductFormats
import ch.openolitor.core.JSONSerializable

trait ReportsEventStoreSerializer extends ReportsJsonProtocol with EntityStoreJsonProtocol with AutoProductFormats[JSONSerializable] {
  import ch.openolitor.core.eventsourcing.events._

  // V1 persisters
  implicit val reportCreatePersister = persister[ReportCreate]("report-create")
  implicit val reportModifyPersister = persister[ReportModify]("report-modify")

  implicit val reportIdPersister = persister[ReportId]("report-id")

  val reportsPersisters = List(
    reportCreatePersister,
    reportModifyPersister,
    reportIdPersister
  )
}
