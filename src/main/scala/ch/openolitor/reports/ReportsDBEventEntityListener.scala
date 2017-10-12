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

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.reports.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.reports.repositories.DefaultReportsUpdateRepositoryComponent
import ch.openolitor.reports.repositories.ReportsUpdateRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object ReportsDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultReportsDBEventEntityListener], sysConfig, system)
}

class DefaultReportsDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends ReportsDBEventEntityListener(sysConfig) with DefaultReportsUpdateRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class ReportsDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with ReportsDBMappings with AsyncConnectionPoolContextAware {
  this: ReportsUpdateRepositoryComponent =>
  import ReportsDBEventEntityListener._

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityCreated(personId, entity: Report) => handleReportCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Report) => handleReportDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Report, orig: Report) => handleReportModified(entity, orig)(personId)

    case x =>
  }

  def handleReportModified(rechnung: Report, orig: Report)(implicit personId: PersonId) = {
  }

  def handleReportDeleted(rechnung: Report)(implicit personId: PersonId) = {
  }

  def handleReportCreated(rechnung: Report)(implicit personId: PersonId) = {
  }
}
